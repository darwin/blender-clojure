try:
    import STPyV8
except ModuleNotFoundError as e:
    print("Unable to import STPyV8 module")
    print("Please make sure you have compiled STPyV8 see ./scripts/build_v8.sh and readme")
    print()
    raise e


from STPyV8 import JSFunction, JSContext


class JSClass(STPyV8.JSClass):

    def isPrototypeOf(self, obj):
        pass
