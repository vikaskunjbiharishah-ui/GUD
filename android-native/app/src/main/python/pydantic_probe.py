"""Load the real Android pydantic-core extension. Not used by CAS import."""

def probe() -> str:
    import pydantic_core
    from pydantic_core import SchemaValidator

    validator = SchemaValidator({"type": "int"})
    if validator.validate_python("7") != 7:
        raise AssertionError("SchemaValidator did not coerce an int")
    return f"{pydantic_core.__version__}|ok"
