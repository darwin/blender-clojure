# various parts of this file were taken from
# https://github.com/buffer/thug
# license: GPL-2.0, see https://github.com/buffer/thug/blob/master/LICENSE.txt
#
# we just need to implement bare minimum for shadow-cljs client to work
#

import inspect
import os
import random
import time

from bclj import v8
import sys
import bs4
import six
from six import StringIO
import six.moves.urllib.parse as urlparse
from lxml.html import builder as E
from lxml.html import tostring
import sched

sched = sched.scheduler(time.time, time.sleep)

from bclj import ws, v8

JSClass = v8.JSClass


class WebSocket(JSClass, ws.WebSocket):

    def __init__(self, url):
        ws.WebSocket.__init__(self, url)


class DOMException(RuntimeError, JSClass):
    def __init__(self, code):
        self.code = code

    # ExceptionCode
    INDEX_SIZE_ERR = 1  # If index or size is negative, or greater than the allowed value
    DOMSTRING_SIZE_ERR = 2  # If the specified range of text does not fit into a DOMString
    HIERARCHY_REQUEST_ERR = 3  # If any node is inserted somewhere it doesn't belong
    WRONG_DOCUMENT_ERR = 4  # If a node is used in a different document than the one that created it (that doesn't support it)
    INVALID_CHARACTER_ERR = 5  # If an invalid or illegal character is specified, such as in a name.
    NO_DATA_ALLOWED_ERR = 6  # If data is specified for a node which does not support data
    NO_MODIFICATION_ALLOWED_ERR = 7  # If an attempt is made to modify an object where modifications are not allowed
    NOT_FOUND_ERR = 8  # If an attempt is made to reference a node in a context where it does not exist
    NOT_SUPPORTED_ERR = 9  # If the implementation does not support the type of object requested
    INUSE_ATTRIBUTE_ERR = 10  # If an attempt is made to add an attribute that is already in use elsewhere

    # Introduced in Level 2
    INVALID_STATE_ERR = 11  # If an attempt is made to use an object that is not, or is no longer, usable
    SYNTAX_ERR = 12  # If an invalid or illegal string is specified
    INVALID_MODIFICATION_ERR = 13  # If an attempt is made to modify the type of the underlying object
    NAMESPACE_ERR = 14  # If an attempt is made to create or change an object in a way which is incorrect with regards to namespaces
    INVALID_ACCESS_ERR = 15  # If a parameter or an operation is not supported by the underlying object


def add_event_listener(self, name, f, *args):
    pass


class Location(JSClass):
    def __init__(self, window):
        self._window = window

    def toString(self):  # pragma: no cover
        return self._window.colorize_url

    @property
    def parts(self):
        return urlparse.urlparse(self._window.colorize_url)

    def get_href(self):
        return self._window.colorize_url

    href = property(get_href)  # , set_href)

    @property
    def protocol(self):
        return self.parts.scheme

    @property
    def host(self):
        return self.parts.netloc

    @property
    def hostname(self):
        return self.parts.hostname

    @property
    def port(self):
        return self.parts.port

    @property
    def pathname(self):
        return self.parts.path

    @property
    def search(self):
        return self.parts.query

    @property
    def hash(self):
        return self.parts.fragment

    def assign(self, url):
        """Loads a new HTML document."""
        self._window.open(url)

    def reload(self, force=False):
        """Reloads the current page."""
        self._window.open(self._window.colorize_url)

    def replace(self, url):
        """Replaces the current document by loading another document at the specified URL."""
        self._window.open(url)


class Window(JSClass):
    class Timer(object):
        max_loops = 3
        max_timers = 16

        def __init__(self, window, code, delay, repeat, lang='JavaScript'):
            self.window = window
            self.code = code
            self.delay = float(delay) / 1000
            self.repeat = repeat
            self.lang = lang
            self.running = True
            self.loops = self.__init_loops()

        def __init_loops(self):
            max_loops = self.max_loops - 1

            if not self.delay:
                return max_loops

            loops = int(0.1 * 600 / self.delay)
            return min(loops, max_loops)

        def start(self):
            self.event = sched.enter(self.delay, 1, self.execute, ())

            try:
                sched.run()
            except Exception as e:  # pragma: no cover
                print("[Timer] Scheduler error: %s", str(e))

        def stop(self):
            self.running = False

            if self.event in sched.queue:  # pragma: no cover
                sched.cancel(self.event)

        def execute(self):
            if len(self.window.timers) > self.max_timers:
                self.running = False

            if not self.running:
                return None

            with self.window.context as ctx:
                try:
                    if isinstance(self.code, v8.JSFunction):
                        self.code()
                    else:
                        ctx.eval(self.code)
                except Exception as e:  # pragma: no cover
                    print("Error while handling timer callback", e)

                    return None

            if self.repeat and self.loops > 0:
                self.loops -= 1
                self.event = sched.enter(self.delay, 1, self.execute, ())

    def __init__(self, url, navigator=None, personality='winxpie60', name="",
                 target='_blank', parent=None, opener=None, replace=False, screen=None,
                 width=800, height=600, left=0, top=None, **kwds):
        self.url = url
        self.doc = Document(bs4.BeautifulSoup('', 'lxml'))
        self.document = self.doc
        self.context = v8.JSContext(self)

        self.WebSocket = WebSocket
        self.WebSocket.window = self

        self.doc.window = self
        self.doc.contentWindow = self

        # for p in w3c_bindings:
        #     setattr(self, p, w3c_bindings[p])

        # self._navigator = navigator if navigator else Navigator(personality, self)
        self._location = Location(self)
        # self._history   = parent.history if parent and parent.history else History(self)

        # if url not in ('about:blank', ):
        #     self._history.update(url, replace)

        self.doc.location = property(self.getLocation, self.setLocation)

        self._target = target
        self._parent = parent if parent else self
        self._opener = opener
        # self._screen = screen or Screen(width, height, 32)
        self._closed = False

        # self._personality = personality
        # self.__init_window_personality()

        self.name = name
        self._left = left
        self._top = top if top else self
        self._screen_top = random.randint(0, 30)
        self.innerWidth = width
        self.innerHeight = height
        self.outerWidth = width
        self.outerHeight = height
        self.timers = []
        # self.java          = java()

        self._symbols = set()
        self._methods = tuple()

    # TODO: implement this?
    addEventListener = add_event_listener

    def getLocation(self):
        """the Location object for the window"""
        return self._location

    def setLocation(self, location):
        self._location.href = location

    location = property(getLocation, setLocation)

    def setTimeout(self, f, delay=0, lang='JavaScript'):
        """
        Sets a delay for executing a function.
        Syntax

        ID = window.setTimeout("funcName", delay)

        Parameters

        funcName is the name of the function for which you want to set a
        delay.

        delay is the number of milliseconds (thousandths of a second)
        that the function should be delayed.

        ID is the interval ID.
        """
        timer = Window.Timer(self, f, delay, False, lang)
        self.timers.append(timer)
        timer.start()

        return len(self.timers) - 1

    def clearTimeout(self, timeoutID):
        """
        Clears the delay set by window.setTimeout().
        Syntax

        window.clearTimeout(timeoutID)

        Parameters

        timeoutID is the ID of the timeout you wish you clear.
        """
        if timeoutID < len(self.timers):
            self.timers[timeoutID].stop()


class EventTarget(object):
    pass


def node_wrap(doc, obj):
    if obj is None:
        return None

    # if isinstance(obj, bs4.CData): # pragma: no cover
    #     return CDATASection(doc, obj)

    if isinstance(obj, bs4.NavigableString):
        return Text(doc, obj)

    return Element(doc, obj)


class Node(JSClass, EventTarget):
    # NodeType
    ELEMENT_NODE = 1
    ATTRIBUTE_NODE = 2
    TEXT_NODE = 3
    CDATA_SECTION_NODE = 4
    ENTITY_REFERENCE_NODE = 5
    ENTITY_NODE = 6
    PROCESSING_INSTRUCTION_NODE = 7
    COMMENT_NODE = 8
    DOCUMENT_NODE = 9
    DOCUMENT_TYPE_NODE = 10
    DOCUMENT_FRAGMENT_NODE = 11
    NOTATION_NODE = 12

    def __init__(self, doc):
        self.doc = doc

    @property
    def firstChild(self):
        return Node.wrap(self.doc, self.tag.contents[0]) if self.tag.contents else None

    @property
    def lastChild(self):
        return Node.wrap(self.doc, self.tag.contents[-1]) if self.tag.contents else None

    @property
    def nextSibling(self):
        return Node.wrap(self.doc, self.tag.next_sibling)

    @property
    def previousSibling(self):
        return Node.wrap(self.doc, self.tag.previous_sibling)

    @property
    def parentNode(self):
        return Node.wrap(self.doc, self.tag.parent) if self.tag.parent else None

    @property
    def innerText(self):
        return str(self.tag.string)

    def is_readonly(self, node):
        return node.nodeType in (Node.DOCUMENT_TYPE_NODE,
                                 Node.NOTATION_NODE,
                                 Node.ENTITY_REFERENCE_NODE,
                                 Node.ENTITY_NODE,)

    def is_text(self, node):
        return node.nodeType in (Node.TEXT_NODE,
                                 Node.PROCESSING_INSTRUCTION_NODE,
                                 Node.CDATA_SECTION_NODE,)

    def appendChild(self, newChild):
        # NO_MODIFICATION_ALLOWED_ERR: Raised if this node is readonly
        if self.is_readonly(self):
            raise DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR)

        if self.is_text(self):
            raise DOMException(DOMException.HIERARCHY_REQUEST_ERR)

        if not newChild:
            raise DOMException(DOMException.HIERARCHY_REQUEST_ERR)

        if not isinstance(newChild, Node):
            raise DOMException(DOMException.HIERARCHY_REQUEST_ERR)

        # If the newChild is already in the tree, it is first removed
        if getattr(newChild, 'tag', None) and newChild.tag in self.tag.contents:
            newChildHash = hash(newChild.tag._node)

            for p in self.tag.contents:
                if getattr(p, '_node', None) is None:
                    continue

                if newChildHash == hash(p._node):
                    p.extract()

        if self.is_text(newChild):
            self.tag.append(newChild.data.output_ready(formatter=lambda x: x))
            return newChild

        if newChild.nodeType in (Node.COMMENT_NODE,):
            self.tag.append(newChild.data)
            return newChild

        if newChild.nodeType in (Node.DOCUMENT_FRAGMENT_NODE,):
            node = self.tag
            for p in newChild.tag.find_all_next():
                node.append(p)
                node = p

            return newChild

        self.tag.append(newChild.tag)
        return newChild

    @staticmethod
    def wrap(doc, obj):
        return node_wrap(doc, obj)


class CharacterData(Node):
    def __init__(self, doc, tag):
        self.tag = tag
        self.tag._node = self
        Node.__init__(self, doc)

    def getData(self):
        return self._data

    def setData(self, data):
        self._data = data

    data = property(getData, setData)

    @property
    def length(self):
        return len(self.data)

    def substringData(self, offset, count):
        return self.data[offset:offset + count]

    def appendData(self, arg):
        self.data += arg

    def insertData(self, offset, arg):
        if offset > len(self.data):
            raise DOMException(DOMException.INDEX_SIZE_ERR)

        self.data = self.data[:offset] + arg + self.data[offset:]

    def deleteData(self, offset, count):
        length = len(self.data)

        if offset > length:
            raise DOMException(DOMException.INDEX_SIZE_ERR)

        if offset + count > length:
            self.data = self.data[:offset]
        else:
            self.data = self.data[:offset] + self.data[offset + count:]

    def replaceData(self, offset, count, arg):
        s = self.data[:offset] + arg + self.data[offset + count:]
        self.data = s


class Text(CharacterData):
    def __init__(self, doc, tag):
        self.data = tag
        CharacterData.__init__(self, doc, tag)

    def splitText(self, offset):
        raise DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR)

    def getNodeValue(self):
        return str(self.data)

    def setNodeValue(self, value):
        self.data = value

    nodeValue = property(getNodeValue, setNodeValue)

    @property
    def nodeName(self):
        return "#text"

    @property
    def nodeType(self):
        return Node.TEXT_NODE


class CSSStyleDeclaration(JSClass):
    def __init__(self, style):
        self.props = dict()

        for prop in [p for p in style.split(';') if p]:
            k, v = prop.strip().split(':')
            self.props[k.strip()] = v.strip()

    @property
    def cssText(self):
        css_text = '; '.join(["%s: %s" % (k, v) for k, v in self.props.items()])
        return css_text + ';' if css_text else ''

    def getPropertyValue(self, name):
        return self.props.get(name, '')

    def removeProperty(self, name):
        return self.props.pop(name, '')

    @property
    def length(self):
        return len(self.props)

    def item(self, index):
        if index < 0 or index >= len(self.props):
            return ''

        return list(self.props.keys())[index]

    def __getattr__(self, name):
        return self.getPropertyValue(name)

    def __setattr__(self, name, value):
        if name in ('props',):
            super(CSSStyleDeclaration, self).__setattr__(name, value)
        else:
            self.props[name] = value


class ElementCSSInlineStyle(object):
    def __init__(self, doc, tag):
        self.doc = doc
        self.tag = tag

        self._style = None

    @property
    def style(self):
        if self._style is None:
            self._style = CSSStyleDeclaration(self.tag['style'] if self.tag.has_attr('style') else '')

        return self._style


class Element(Node, ElementCSSInlineStyle):
    def __init__(self, doc, tag):
        self.tag = tag
        self.tag._node = self
        Node.__init__(self, doc)
        ElementCSSInlineStyle.__init__(self, doc, tag)

    @property
    def nodeType(self):
        return Node.ELEMENT_NODE

    @property
    def tagName(self):
        return self.tag.name.upper()


def attr_property(name, attrtype=str, readonly=False, default=None):
    def getter(self):
        return attrtype(self.tag[name]) if self.tag.has_attr(name) else default

    def setter(self, value):
        if attrtype in six.integer_types and value.endswith('px'):
            value = value.split('px')[0]

        self.tag[name] = attrtype(value)

    return property(getter) if readonly else property(getter, setter)


class HTMLElement(Element):
    id = attr_property("id")
    title = attr_property("title")
    lang = attr_property("lang")
    dir = attr_property("dir")
    className = attr_property("class", default="")

    def __init__(self, doc, tag):
        Element.__init__(self, doc, tag)

    def getInnerHTML(self):
        if not self.hasChildNodes():
            return ""

        html = StringIO()

        for tag in self.tag.contents:
            html.write(str(tag))

        return html.getvalue()

    def setInnerHTML(self, html):
        self.tag.clear()

        for node in bs4.BeautifulSoup(html, "html.parser").contents:
            self.tag.append(node)

            name = getattr(node, 'name', None)
            if name is None:
                continue

    def getOuterHTML(self):
        return str(self.tag)

    innerHTML = property(getInnerHTML, setInnerHTML)
    outerHTML = property(getOuterHTML, setInnerHTML)

    # WARNING: NOT DEFINED IN W3C SPECS!
    def focus(self):
        pass

    @property
    def sourceIndex(self):
        return None

    @property
    def offsetWidth(self):
        return random.randint(10, 100)

    @property
    def offsetTop(self):
        return random.randint(1, 10)

    def insertAdjacentHTML(self, position, text):
        if position not in ('beforebegin', 'afterbegin', 'beforeend', 'afterend',):
            raise DOMException(DOMException.NOT_SUPPORTED_ERR)

        if position in ('beforebegin',):
            target = self.tag.parent if self.tag.parent else self.doc.find('body')
            pos = target.index(self.tag) - 1
        if position in ('afterbegin',):
            target = self.tag
            pos = 0
        if position in ('beforeend',):
            target = self.tag
            pos = len(list(self.tag.children))
        if position in ('afterend',):
            target = self.tag.parent if self.tag.parent else self.doc.find('body')
            pos = target.index(self.tag) + 1

        for node in bs4.BeautifulSoup(text, "html.parser").contents:
            target.insert(pos, node)
            pos += 1


class Document(JSClass):

    def __init__(self, doc):
        Node.__init__(self, doc)

    def createElement(self, tagname, tagvalue=None):
        return DOMImplementation.createHTMLElement(self, bs4.Tag(parser=self.doc, name=tagname))

    def getElementById(self, elementId):
        tag = self.doc.find(id=elementId)
        return DOMImplementation.createHTMLElement(self, tag) if tag else None


def text_property(readonly=False):
    def getter(self):
        return str(self.tag.string) if self.tag.string else ""

    def setter(self, text):
        self.tag.string = text

        if self.tagName.lower() in ('script',):
            self.doc.root.evalScript(text, self.tag.string)

    return property(getter) if readonly else property(getter, setter)


class HTMLCollection(JSClass):
    def __init__(self, doc, nodes):
        self.doc = doc
        self.nodes = nodes

    def __len__(self):
        return self.length

    def __getitem__(self, key):
        return self.item(int(key))

    def __delitem__(self, key):  # pragma: no cover
        self.nodes.__delitem__(key)

    def __getattr__(self, key):
        return self.namedItem(key)

    @property
    def length(self):
        return len(self.nodes)

    def item(self, index):
        if index < 0 or index >= self.length:
            return None

        if isinstance(self.nodes[index], bs4.element.Tag):
            return DOMImplementation.createHTMLElement(self.doc, self.nodes[index])

        return self.nodes[index]

    def namedItem(self, name):
        for node in self.nodes:
            if 'id' in node.attrs and node.attrs['id'] in (name,):
                return DOMImplementation.createHTMLElement(self.doc, node)

        for node in self.nodes:
            if 'name' in node.attrs and node.attrs['name'] in (name,):
                return DOMImplementation.createHTMLElement(self.doc, node)

        return None


class HTMLDocument(Document):
    innerHTML = text_property()

    def __str__(self):
        return "[object HTMLDocument]"

    def __init__(self, doc, win=None, referer=None, lastModified=None, cookie=''):
        Document.__init__(self, doc)

        self._win = win
        self._body = None  # HTMLBodyElement(self.doc, self.doc.find('body'))
        self._referer = referer
        self._lastModified = lastModified
        self._cookie = cookie
        self._html = None
        self._head = None
        self._currentScript = None
        self._readyState = "loading"
        self._domain = urlparse.urlparse(self._win.colorize_url).hostname if self._win else ''
        self.current = None
        self.all = self._all
        self.implementation.createHTMLDocument = self.implementation._createHTMLDocument

    def getWindow(self):
        return self._win

    def setWindow(self, win):
        self._win = win

    window = property(getWindow, setWindow)


class DOMImplementation(HTMLDocument):
    TAGS = {
        # "html"          : HTML.HTMLHtmlElement,
        # "head"          : HTML.HTMLHeadElement,
        # "link"          : HTML.HTMLLinkElement,
        # "title"         : HTML.HTMLTitleElement,
        # "meta"          : HTML.HTMLMetaElement,
        # "base"          : HTML.HTMLBaseElement,
        # "isindex"       : HTML.HTMLIsIndexElement,
        # "style"         : HTML.HTMLStyleElement,
        # "body"          : HTML.HTMLBodyElement,
        # "form"          : HTML.HTMLFormElement,
        # "select"        : HTML.HTMLSelectElement,
        # "optgroup"      : HTML.HTMLOptGroupElement,
        # "option"        : HTML.HTMLOptionElement,
        # "input"         : HTML.HTMLInputElement,
        # "textarea"      : HTML.HTMLTextAreaElement,
        # "button"        : HTML.HTMLButtonElement,
        # "label"         : HTML.HTMLLabelElement,
        # "fieldset"      : HTML.HTMLFieldSetElement,
        # "legend"        : HTML.HTMLLegendElement,
        # "ul"            : HTML.HTMLUListElement,
        # "ol"            : HTML.HTMLOListElement,
        # "dl"            : HTML.HTMLDListElement,
        # "dir"           : HTML.HTMLDirectoryElement,
        # "menu"          : HTML.HTMLMenuElement,
        # "li"            : HTML.HTMLLIElement,
        # "div"           : HTML.HTMLDivElement,
        # "p"             : HTML.HTMLParagraphElement,
        # "h1"            : HTML.HTMLHeadingElement,
        # "h2"            : HTML.HTMLHeadingElement,
        # "h3"            : HTML.HTMLHeadingElement,
        # "h4"            : HTML.HTMLHeadingElement,
        # "h5"            : HTML.HTMLHeadingElement,
        # "h6"            : HTML.HTMLHeadingElement,
        # "q"             : HTML.HTMLQuoteElement,
        # "blockquote"    : HTML.HTMLQuoteElement,
        # "span"          : HTML.HTMLSpanElement,
        # "pre"           : HTML.HTMLPreElement,
        # "br"            : HTML.HTMLBRElement,
        # "basefont"      : HTML.HTMLBaseFontElement,
        # "font"          : HTML.HTMLFontElement,
        # "hr"            : HTML.HTMLHRElement,
        # "ins"           : HTML.HTMLModElement,
        # "del"           : HTML.HTMLModElement,
        # "a"             : HTML.HTMLAnchorElement,
        # "object"        : HTML.HTMLObjectElement,
        # "param"         : HTML.HTMLParamElement,
        # "img"           : HTML.HTMLImageElement,
        # "applet"        : HTML.HTMLAppletElement,
        # "script"        : HTML.HTMLScriptElement,
        # "frameset"      : HTML.HTMLFrameSetElement,
        # "frame"         : HTML.HTMLFrameElement,
        # "iframe"        : HTML.HTMLIFrameElement,
        # "table"         : HTML.HTMLTableElement,
        # "caption"       : HTML.HTMLTableCaptionElement,
        # "col"           : HTML.HTMLTableColElement,
        # "colgroup"      : HTML.HTMLTableColElement,
        # "thead"         : HTML.HTMLTableSectionElement,
        # "tbody"         : HTML.HTMLTableSectionElement,
        # "tfoot"         : HTML.HTMLTableSectionElement,
        # "tr"            : HTML.HTMLTableRowElement,
        # "th"            : HTML.HTMLTableCellElement,
        # "td"            : HTML.HTMLTableCellElement,
        # "media"         : HTML.HTMLMediaElement,
        # "audio"         : HTML.HTMLAudioElement,
    }

    @staticmethod
    def createHTMLElement(doc, tag):
        # if isinstance(tag, bs4.NavigableString):
        #     return Node.wrap(doc, tag)

        # if tag.name.lower() in DOMImplementation.TAGS:
        #     return DOMImplementation.TAGS[tag.name.lower()](doc, tag)

        return HTMLElement(doc, tag)

    def _createHTMLDocument(self, title=None):
        body = E.BODY()
        title = E.TITLE(title) if title else ""
        head = E.HEAD(title)
        html = E.HTML(head, body)

        soup = bs4.BeautifulSoup(tostring(html, doctype='<!doctype html>'), "lxml")
        return DOMImplementation(soup)
