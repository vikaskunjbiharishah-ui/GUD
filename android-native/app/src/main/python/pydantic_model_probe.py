"""Real pydantic v2 on the Android pydantic-core wheel. Not a CAS parse."""

from datetime import date
from decimal import Decimal


def probe() -> str:
    import android_pydantic
    import pydantic
    from pydantic import BaseModel, ConfigDict, Field, ValidationError

    if BaseModel is android_pydantic.BaseModel:
        raise AssertionError("real pydantic was shadowed by the Android stub")
    if "android_pydantic" in (pydantic.__file__ or ""):
        raise AssertionError(f"unexpected pydantic origin: {pydantic.__file__}")

    class StatementPeriod(BaseModel):
        from_: str = Field(alias="from")
        to: str
        model_config = ConfigDict(populate_by_name=True)

    class Holding(BaseModel):
        units: Decimal
        nav_date: date

    period = StatementPeriod.model_validate({"from": "01-Jan-2024", "to": "31-Dec-2024"})
    if period.from_ != "01-Jan-2024" or period.to != "31-Dec-2024":
        raise AssertionError("alias populate failed")
    dumped = period.model_dump(by_alias=True)
    if dumped != {"from": "01-Jan-2024", "to": "31-Dec-2024"}:
        raise AssertionError(f"model_dump alias failed: {dumped}")

    holding = Holding.model_validate({"units": "10.50", "nav_date": "2024-06-30"})
    if holding.units != Decimal("10.50") or holding.nav_date != date(2024, 6, 30):
        raise AssertionError("decimal/date coercion failed")
    try:
        Holding.model_validate({"units": "nope", "nav_date": "2024-06-30"})
    except ValidationError:
        pass
    else:
        raise AssertionError("invalid decimal was accepted")

    return f"{pydantic.__version__}|{BaseModel.__module__}|ok"
