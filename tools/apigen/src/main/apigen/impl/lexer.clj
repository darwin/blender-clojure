(ns apigen.impl.lexer
  (:require [blancas.kern.core :refer :all]
            [blancas.kern.i18n :refer [i18n]]
            [clojure.string :as string]
            [apigen.impl.kern :refer :all]
            [apigen.impl.text :as text]))

; -- low level parsers ------------------------------------------------------------------------------------------------------

(def space-ascii 32)

; parses an escape code for a basic char
(def esc-char
  (let [codes (zipmap "btnfr'\"\\/" "\b\t\n\f\r'\"\\/")]
    (>>= (<?> (one-of* "btnfr'\"\\/") (i18n :esc-code))
         (fn [x] (return (get codes x))))))

; parses an unquoted character literal. Character c must be escaped
(defn basic-char [c]
  (<?> (<|> (satisfy #(and (not= % c) (not= % \\) (>= (int %) space-ascii)))
            (>> (sym* \\) esc-char))
       (i18n :char-lit)))

(defn str-parser [lex qc f]
  (<?> (lex (between (sym* qc)
                     (<?> (sym* qc) "end of python string literal")
                     (<+> (many (f qc)))))
       "python string literal"))

; -- kern rules -------------------------------------------------------------------------------------------------------------

(def sentence-char
  any-char)

(defn sentence [sep]
  (<?> (lexeme (<+> (many-till sentence-char (predict sep))))
       "sentence"))

(def py-none-lit
  (>> (word "None") (return ::nil)))

(def py-bool-lit
  (<|> (>> (word "True") (return true))
       (>> (word "False") (return false))))

(def py-int-lit
  dec-lit)

(def py-float-lit
  ; TODO: handle scientific notation
  float-lit)

(def py-sq-string-lit
  (str-parser lexeme \' basic-char))

(def py-dq-string-lit
  (str-parser lexeme \" basic-char))

(def py-star-lit
  (>> (sym* \*) (return "*")))

(def py-builtin
  (bind [v (angles (sentence (sym* \>)))]
    (return (str "<" v ">"))))

(defn translate-known-raw-values [v]
  (case v
    "inf" ##Inf
    "-inf" ##-Inf
    "NaN" ##NaN
    v))

(def py-raw
  (bind [v (sentence (<|> (sym* \,) (sym* \]) eof))]
    (return (let [clean-v (string/trim v)]
              (or (translate-known-raw-values clean-v)
                  (str "py:" clean-v))))))

(declare py-value)

; https://github.com/blancas/kern/issues/19
(defn get-py-value [s]
  (py-value s))

(defn empty-pair [open close res]
  (<:> (bind [_ (<*> open close)]
         (return res))))

(def py-list
  (bind [v (<|> (empty-pair (sym \() (sym \)) '())
                (parens (comma-sep get-py-value)))]
    (return (into '() v))))

(def py-set
  (bind [v (<|> (empty-pair (sym \{) (sym \}) nil)
                (braces (comma-sep get-py-value)))]
    (return (set v))))

(def py-vector
  (bind [v (<|> (empty-pair (sym \[) (sym \]) nil)
                (brackets (comma-sep get-py-value)))]
    (return (vec v))))

(def py-union
  (bind [v (>> (word "Union") trim get-py-value)]
    (return {:kind  :union
             :value v})))

(def py-value
  (<|> py-none-lit
       py-bool-lit
       py-int-lit
       py-float-lit
       py-sq-string-lit
       py-dq-string-lit
       py-union
       py-list
       py-vector
       py-set
       py-builtin
       py-raw))

(def signature-param
  (bind [name (<|> identifier py-star-lit)
         default-value (optional (>> (sym* \=) trim py-value))]
    (return (cond-> {:name name}
                    (some? default-value) (assoc :default default-value)
                    (= default-value ::nil) (assoc :default nil)))))

(def signature-param-list
  (>> trim
      (comma-sep signature-param)))

; type names are quite messy, we allow extra punctuation
(def type-name-relax-chars " .,_-*:/")

(defn cleanup-type-name [s]
  (-> s
      (text/trim type-name-relax-chars)
      (string/replace "," ", ")))

(def type-name
  (bind [name (<+> (many (<|> alpha-num
                              (one-of type-name-relax-chars))))]
    (return (cleanup-type-name name))))

(def gl-params
  (bind [_ (sym \()
         params (sentence (sym \)))
         _ (sym \))]
    (return {:params params})))

(def gl-prototype
  (bind [_ (sym \$)
         _ (sym \{)
         return-type (sentence (sym \}))
         _ (sym \})
         params (optional gl-params)]
    (return (merge {:kind   :gl-prototype
                    :return return-type}
                   params))))

(def type-domain
  (bind [in-list (optional (brackets (comma-sep py-value)))
         in-interface (optional gl-prototype)
         in-set (optional (braces (comma-sep py-value)))]
    (return (or in-interface
                (if in-list
                  (vec in-list)
                  (if in-set
                    (set in-set)))))))

(def flag-token
  (<|> alpha-num (one-of* " '.") (sym* \’) (sym* \‘)))

(def flag-name
  (bind [name (<+> (many flag-token))]
    (return (string/trim name))))

(def type-flags
  (parens (comma-sep flag-name)))

(defn assoc-type-flags [m flags]
  (cond-> m
          (contains? flags "optional") (assoc :optional? true)
          (not (empty? flags)) (assoc :type-flags flags)))

(def type-spec
  (bind [name type-name
         domain type-domain
         default (optional (<:> (>> trim (optional (sym \,)) trim (word "default") trim py-value)))
         flags (optional (<:> (>> trim (optional (sym \,)) type-flags)))]
    (return (cond-> {:type name}
                    (some? default) (assoc :default default)
                    (some? domain) (assoc :domain domain)
                    true (assoc-type-flags (set flags))))))

(def remainder
  (sentence eof))

(def param-soup
  (bind [name (>> trim identifier)
         type (optional (parens type-spec))
         doc (>> trim (optional (sym* \–)) trim remainder)]
    (return (merge {:name name
                    :doc  doc}
                   type))))

; -- lexing -----------------------------------------------------------------------------------------------------------------

(defn lexer-error-str [res]
  (with-out-str
    (print-error res)))

(defn error-pointer-str [res]
  (let [{:keys [line col]} (get-in res [:error :pos])]
    (if (and (= line 1) (number? col))
      (str (apply str (repeat (dec col) " ")) "^\n"))))

(defn try-parse [lexer value]
  (try
    (parse lexer value)
    (catch Throwable e
      (throw (ex-info (str "internal lexer exception: " (.getMessage e) "\n" value)
                      {:value value}
                      e)))))

(defn lex [lexer value]
  (let [res (try-parse lexer value)]
    (if (:ok res)
      (:value res)
      (throw (ex-info (str "unable to parse\n" value "\n"
                           (error-pointer-str res)
                           (lexer-error-str res))
                      {:res res})))))

; -- API --------------------------------------------------------------------------------------------------------------------

(defn parse-params-signature [signature]
  (lex signature-param-list signature))

(defn separate-doc-if-possible [soup]
  (if-some [i (string/index-of soup "–")]
    [(string/trim (subs soup 0 i))
     (string/trim (subs soup (inc i)))]
    (if-some [i (string/last-index-of soup ")")]
      [(string/trim (subs soup 0 (inc i)))
       (string/trim (subs soup (inc i)))]
      [soup])))

(defn massage-soup [soup]
  (-> soup
      (string/replace "I{" "${")))

(defn peal-parens-off [s]
  (if-some [m (re-matches #"\((.*)\)" s)]
    (second m)
    s))

(defn beautify-type-spec [s]
  (-> s
      (string/replace #"\s*([.,(){}\[\]])\s*" "$1")
      (string/replace #"([({\[])" " $1")))

(defn clean-type-spec [s]
  (-> s
      (string/trim)
      (peal-parens-off)
      (string/trim)
      (beautify-type-spec)))

(defn parse-soup-simple [meat doc]
  (let [[name type-spec] (string/split meat #" " 2)]
    {:name      name
     :type-spec (if type-spec (clean-type-spec type-spec))
     :doc       doc}))

(defn parse-param-soup [soup]
  ; this is to prevent stack overflow when parsing long docs
  (let [[meat doc] (separate-doc-if-possible soup)
        simple-param (parse-soup-simple meat doc)]
    simple-param
    ; this is for future if we wanted to parse soup more precisely
    #_(update (lex param-soup (massage-soup meat)) :doc (fn [parsed-doc] (str parsed-doc doc)))))

; ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

(comment

  (parse-params-signature (str " segments=32 , ring_count,radius=1.0,calc_uvs=True,"
                               "enter_editmode=False,xyz=None ,align='WORLD',*,location= ( 0.0, 0.0 ,0.0 ) ,"
                               "some_set={'A','B'},fn=<built-in-fn something >,major=bpy.app.version[0] "))

  (parse-params-signature "location=(0)")

  (parse-params-signature "input_filepath=\"\",output_filepath=\"\"")
  (parse-params-signature "color=(0.0,0.0,0.0,1.0)")
  (parse-params-signature "type='BASE_COLOR',name=\"Untitled\",width=1024,height=1024,color=(0.0,0.0,0.0,1.0),alpha=True,generated_type='BLANK',float=False")
  (parse-params-signature "bb_quality=True,align_mode='OPT_2',relative_to='OPT_4',align_axis={}")
  (parse-params-signature "align_axis={}")

  (parse-param-soup "abcefg (xx [1, 2], (optional) ) dad \n        sdadsa asds")
  (parse-param-soup "abcefg xdasad")
  (parse-param-soup "vertex_only ( boolean , ( optional ) ) – Vertex Only, Bevel only vertices")
  (parse-param-soup (str "material ( int in [ -1 , inf ] , ( optional ) ) – Material, Material for bevel"
                         "faces (-1 means use adjacent faces)"))
  (parse-param-soup (str "delimit ( enum set in {'NORMAL' , 'MATERIAL' , 'SEAM' , 'SHARP' , 'UV'} , "
                         "( optional ) ) – Delimit, Delimit dissolve operation NORMAL Normal, Delimit "
                         "by face directions. MATERIAL Material, Delimit by face material. SEAM Seam, "
                         "Delimit by edge seams. SHARP Sharp, Delimit by sharp edges. UV UVs, Delimit by UV coordinates."))

  (parse-param-soup (str "absolute (boolean [1, 2, 3] (optional, internal)) – When"
                         "true the paths returned are made absolute."))
  (parse-param-soup (str "some_name (enum in ['LESS', 'EQUAL', 'GREATER', 'NOTEQUAL'], (optional))"
                         "– Type, Type of comparison to make"))
  (parse-param-soup "time (int, float or datetime.timedelta .) – time in seconds.")
  (parse-param-soup (str "MESH_OT_duplicate ( MESH_OT_duplicate , (optional)) – Duplicate, "
                         "Duplicate selected vertices, edges or faces"))
  (parse-param-soup (str "input_filepath ( string , ( optional , never None ) ) – Input Filepath, File "
                         "path for image to denoise. If not specified, uses the render file path and frame "
                         "range from the scene"))


  (parse-param-soup "function ( Callable [ [ ] , Union [ float , None ] ] ) – The function that should called.")

  (def huge-soup (str "type ( enum in [ 'DATA_TRANSFER' , 'MESH_CACHE' , 'MESH_SEQUENCE_CACHE' , "
                      "'NORMAL_EDIT' , 'WEIGHTED_NORMAL' , 'UV_PROJECT' , 'UV_WARP' , 'VERTEX_WEIGHT_EDIT' ,"
                      "'VERTEX_WEIGHT_MIX' , 'VERTEX_WEIGHT_PROXIMITY' , 'ARRAY' , 'BEVEL' , 'BOOLEAN' , "
                      "'BUILD' , 'DECIMATE' , 'EDGE_SPLIT' , 'MASK' , 'MIRROR' , 'MULTIRES' , 'REMESH' , "
                      "'SCREW' , 'SKIN' , 'SOLIDIFY' , 'SUBSURF' , 'TRIANGULATE' , 'WIREFRAME' , 'WELD' , "
                      "'ARMATURE' , 'CAST' , 'CURVE' , 'DISPLACE' , 'HOOK' , 'LAPLACIANDEFORM' , 'LATTICE' , "
                      "'MESH_DEFORM' , 'SHRINKWRAP' , 'SIMPLE_DEFORM' , 'SMOOTH' , 'CORRECTIVE_SMOOTH' , "
                      "'LAPLACIANSMOOTH' , 'SURFACE_DEFORM' , 'WARP' , 'WAVE' , 'CLOTH' , 'COLLISION' , "
                      "'DYNAMIC_PAINT' , 'EXPLODE' , 'OCEAN' , 'PARTICLE_INSTANCE' , 'PARTICLE_SYSTEM' ,"
                      "'FLUID' , 'SOFT_BODY' , 'SURFACE' ] , ( optional ) ) – Type DATA_TRANSFER Data Transfer, "
                      "Transfer several types of data (vertex groups, UV maps, vertex colors, custom normals) "

                      "from one mesh to another. MESH_CACHE Mesh Cache, Deform the mesh using an external "
                      "frame-by-frame vertex transform cache. MESH_SEQUENCE_CACHE Mesh Sequence Cache, Deform "
                      "the mesh or curve using an external mesh cache in Alembic format. NORMAL_EDIT Normal Edit, "
                      "Modify the direction of the surface normals. WEIGHTED_NORMAL Weighted Normal, Modify the "
                      "direction of the surface normals using a weighting method. UV_PROJECT UV Project, Project "
                      "the UV map coordinates from the negative Z axis of another object. UV_WARP UV Warp, "
                      "Transform the UV map using the the difference between two objects. VERTEX_WEIGHT_EDIT Vertex "
                      "Weight Edit, Modify of the weights of a vertex group. VERTEX_WEIGHT_MIX Vertex Weight Mix, "
                      "Mix the weights of two vertex groups. VERTEX_WEIGHT_PROXIMITY Vertex Weight Proximity, "
                      "Set the vertex group weights based on the distance to another target object. "
                      "ARRAY Array, Create copies of the shape with offsets. BEVEL Bevel, Generate sloped "
                      "corners by adding geometry to the mesh’s edges or vertices. BOOLEAN Boolean, Use another "
                      "shape to cut, combine or perform a difference operation. BUILD Build, Cause the faces of "
                      "the mesh object to appear or disappear one after the other over time. DECIMATE Decimate, "
                      "Reduce the geometry density. EDGE_SPLIT Edge Split, Split away joined faces at the edges. "
                      "MASK Mask, Dynamically hide vertices based on a vertex group or armature. MIRROR Mirror, "
                      "Mirror along the local X, Y and/or Z axes, over the object origin. MULTIRES Multiresolution, "
                      "Subdivide the mesh in a way that allows editing the higher subdivision levels. REMESH Remesh, "
                      "Generate new mesh topology based on the current shape. SCREW Screw, Lathe around an axis, "
                      "treating the inout mesh as a profile. SKIN Skin, Create a solid shape from vertices and edges, "
                      "using the vertex radius to define the thickness. SOLIDIFY Solidify,  Make the surface thick. "
                      "SUBSURF Subdivision Surface, Split the faces into smaller parts, giving it a smoother appearance. "
                      "TRIANGULATE Triangulate, Convert all polygons to triangles. WIREFRAME Wireframe, Convert faces "
                      "into thickened edges. WELD Weld, Find groups of vertices closer then dist and merges them together. "
                      "ARMATURE Armature, Deform the shape using an armature object. CAST Cast, Shift the shape towards a "
                      "predefined primitive. CURVE Curve, Bend the mesh using a curve object. DISPLACE Displace, Offset "
                      "vertices based on a texture. HOOK Hook, Deform specific points using another object. LAPLACIANDEFORM "
                      "Laplacian Deform, Deform based a series of anchor points. LATTICE Lattice, Deform using the shape of a "
                      "lattice object. MESH_DEFORM Mesh Deform, Deform using a different mesh, which acts as a deformation cage. "
                      "SHRINKWRAP Shrinkwrap, Project the shape onto another object. SIMPLE_DEFORM Simple Deform, Deform "
                      "the shape by twisting, bending, tapering or stretching. SMOOTH Smooth, Smooth the mesh by flattening "
                      "the angles between adjacent faces. CORRECTIVE_SMOOTH Smooth Corrective, Smooth the mesh while "
                      "still preserving the volume. LAPLACIANSMOOTH Smooth Laplacian, Reduce the noise on a mesh surface "
                      "with minimal changes to its shape. SURFACE_DEFORM Surface Deform, Transfer motion from another mesh. "
                      "WARP Warp, Warp parts of a mesh to a new location in a very flexible way thanks to 2 specified objects. "
                      "WAVE Wave, Adds a ripple-like motion to an object’s geometry. CLOTH Cloth. COLLISION Collision. "
                      "DYNAMIC_PAINT Dynamic Paint. EXPLODE Explode, Break apart the mesh faces and let them follow "
                      "particles. OCEAN Ocean, Generate a moving ocean surface. PARTICLE_INSTANCE Particle Instance. "
                      "PARTICLE_SYSTEM Particle System, Spawn particles from the shape. FLUID Fluid Simulation. SOFT_BODY"
                      "Soft Body. SURFACE Surface."))
  (separate-doc-if-possible huge-soup)
  (parse-param-soup huge-soup)
  (parse-param-soup "textures ( bgl.Buffer ${GL_INT})")
  (parse-param-soup "v ( Depends on function prototype. ( only with '2' prototypes ) )")

  ; TODO: this parses domain as flags
  (parse-param-soup "object_action_pairs (Sequence of ( bpy.types.Object , bpy.types.Action ))")
  (parse-param-soup "edge_percents ( dict mapping vert/edge/face types to float )")

  (parse-param-soup "axis ( enum in [ 'X' , 'Y' , 'Z' ] , default 'X' )")
  (parse-param-soup "filemode ( int in [ 1 , 9 ] , ( optional ) )")

  (parse-param-soup "equation ( bgl.Buffer object I{type GL_FLOAT}(double))")

  )
