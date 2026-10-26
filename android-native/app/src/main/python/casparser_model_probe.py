"""Load frozen casparser models on real pydantic. Does not open a PDF."""

import sys
import types
from decimal import Decimal


def _block_pdfium() -> None:
    """Let frozen casparser import without the native PDF library.

    parsers/detect.py imports pypdfium2 at module import, before any page is
    read. This stand-in is process-local and refuses to open a document.
    """
    if "pypdfium2" in sys.modules:
        return
    pdfium = types.ModuleType("pypdfium2")
    helpers = types.ModuleType("pypdfium2._helpers")
    misc = types.ModuleType("pypdfium2._helpers.misc")

    class PdfiumError(Exception):
        pass

    class PdfDocument:
        def __init__(self, *_args, **_kwargs):
            raise RuntimeError("pdfium shim: text extraction is not available")

    misc.PdfiumError = PdfiumError
    pdfium.PdfDocument = PdfDocument
    pdfium._helpers = helpers
    helpers.misc = misc
    sys.modules["pypdfium2"] = pdfium
    sys.modules["pypdfium2._helpers"] = helpers
    sys.modules["pypdfium2._helpers.misc"] = misc


def probe() -> str:
    import android_pydantic
    import pydantic
    from pydantic import BaseModel

    if BaseModel is android_pydantic.BaseModel:
        raise AssertionError("real pydantic was shadowed by the Android stub")

    _block_pdfium()
    import casparser
    from casparser.types import SchemeValuation, StatementPeriod

    if SchemeValuation.__module__ != "casparser.types":
        raise AssertionError(f"unexpected model module: {SchemeValuation.__module__}")
    if not any(cls.__module__ == "pydantic.main" for cls in SchemeValuation.__mro__):
        raise AssertionError("frozen SchemeValuation is not a real pydantic model")

    row = SchemeValuation.model_validate(
        {"date": "2024-06-30", "nav": "1,234.50", "value": "10"}
    )
    if row.nav != Decimal("1234.50") or str(row.date) != "2024-06-30":
        raise AssertionError(f"frozen valuation did not validate: {row}")
    period = StatementPeriod.model_validate({"from": "01-Apr-2024", "to": "30-Jun-2024"})
    if period.model_dump(by_alias=True) != {"from": "01-Apr-2024", "to": "30-Jun-2024"}:
        raise AssertionError("frozen statement period alias failed")
    return f"{casparser.__version__}|{SchemeValuation.__module__}|{row.nav}|ok"
