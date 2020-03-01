import hy
import bpy
import platform


def describe_environment():
    return "{appname} {version} using {py}({build}) {pyversion} on {os} in Blender {blender}".format(
        appname=hy.__appname__,
        version=hy.__version__,
        py=platform.python_implementation(),
        build=platform.python_build()[0],
        pyversion=platform.python_version(),
        os=platform.system(),
        blender=bpy.app.version_string)
