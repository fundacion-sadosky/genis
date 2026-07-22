#!/usr/bin/env python3
"""
Regression detector for the GENis core test tier.

Parses `sbt core/test` output, compares the measured test counts against a
committed baseline, and classifies the run. Designed to be *unpoisonable*:

  * A run that did not compile / did not really run tests is classified as
    ``broken`` and NEVER overwrites the baseline (this is the bug from #298:
    a compile break produced 0 tests, and 0 got saved as the new baseline,
    blinding the detector forever).
  * The baseline is only ever written on a *valid measurement* and only in the
    improving direction (more ``passed`` / fewer ``failed``). It can never
    ratchet down or be reset to zero by a broken run.

Statuses (written to GITHUB_OUTPUT as ``status``):
  broken      -> did not compile / too few tests ran. Alert, do NOT touch baseline.
  regression  -> valid run, but fewer passing or more failing than baseline. Alert.
  seeded      -> no prior baseline; established one from this valid run.
  improved    -> valid run strictly better than baseline; ratchet baseline up.
  ok          -> valid run equal to baseline.

``update_baseline`` output is ``true`` for seeded/improved, else ``false``.
``alert`` output is ``true`` for broken/regression, else ``false``.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys

# A valid measurement must have run at least this many tests. Guards against a
# broken compile / dead harness reporting a handful of tests (or zero). The core
# tier runs ~1000+ tests, so 100 cleanly separates "real run" from "broken".
MIN_TOTAL_ABS = 100

TOTAL_RE = re.compile(r"Total number of tests run:\s*(\d+)")
TESTS_RE = re.compile(r"Tests:\s*succeeded\s*(\d+),\s*failed\s*(\d+)")
ABORTED_RE = re.compile(r"Suites:\s*completed\s*\d+,\s*aborted\s*(\d+)")
COMPILE_FAIL_RE = re.compile(r"Compilation failed|error\] .*(?:is not a member|not found:|cannot|expected)")


def parse_output(text: str) -> dict:
    """Extract test counts from sbt/ScalaTest output. Sums across every summary
    block (a run may emit more than one)."""
    totals = [int(m) for m in TOTAL_RE.findall(text)]
    tests = TESTS_RE.findall(text)
    aborted = [int(m) for m in ABORTED_RE.findall(text)]

    passed = sum(int(s) for s, _ in tests)
    failed = sum(int(f) for _, f in tests)
    total = sum(totals)
    has_summary = bool(totals)

    return {
        "has_summary": has_summary,
        "total": total,
        "passed": passed,
        "failed": failed,
        "aborted_suites": sum(aborted),
    }


def load_baseline(path: str) -> dict | None:
    if not path or not os.path.exists(path):
        return None
    try:
        with open(path, encoding="utf-8") as fh:
            data = json.load(fh)
    except (json.JSONDecodeError, OSError):
        return None
    if not data.get("seeded"):
        return None
    return data


def classify(measured: dict, sbt_exit: int, baseline: dict | None) -> dict:
    total = measured["total"]

    # --- Validity gate: could this run even measure anything? ---------------
    # Not valid if: no summary at all (compile break / harness died), or too
    # few tests ran in absolute terms, or (when we have a baseline) the count
    # collapsed to less than half of it (something structural broke).
    valid = measured["has_summary"] and total >= MIN_TOTAL_ABS
    if valid and baseline is not None:
        if total < baseline.get("total", 0) / 2:
            valid = False

    if not valid:
        reason = "no compiló / no corrió (sin resumen de tests)" if not measured["has_summary"] \
            else f"corrieron sólo {total} tests (mínimo esperado {MIN_TOTAL_ABS} / la mitad del baseline)"
        return {
            "status": "broken",
            "reason": reason,
            "alert": True,
            "update_baseline": False,
        }

    # --- Valid measurement: compare against baseline ------------------------
    if baseline is None:
        return {
            "status": "seeded",
            "reason": "primer baseline válido",
            "alert": False,
            "update_baseline": True,
        }

    b_passed = baseline.get("passed", 0)
    b_failed = baseline.get("failed", 0)

    broke_passing = measured["passed"] < b_passed
    new_failures = measured["failed"] > b_failed

    if broke_passing or new_failures:
        return {
            "status": "regression",
            "reason": "; ".join(
                filter(None, [
                    f"passing bajó {b_passed}→{measured['passed']}" if broke_passing else "",
                    f"failing subió {b_failed}→{measured['failed']}" if new_failures else "",
                ])
            ),
            "alert": True,
            "update_baseline": False,
        }

    improved = measured["passed"] > b_passed or measured["failed"] < b_failed
    return {
        "status": "improved" if improved else "ok",
        "reason": "mejoró respecto al baseline" if improved else "sin cambios respecto al baseline",
        "alert": False,
        "update_baseline": improved,
    }


def emit_github(name_values: dict, path_env: str) -> None:
    path = os.environ.get(path_env)
    if not path:
        return
    with open(path, "a", encoding="utf-8") as fh:
        for k, v in name_values.items():
            fh.write(f"{k}={v}\n")


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--output", required=True, help="captured sbt/ScalaTest output file")
    ap.add_argument("--sbt-exit", type=int, default=0, help="exit code of the sbt process")
    ap.add_argument("--baseline", default=".github/test-baseline.json")
    ap.add_argument("--write-baseline", action="store_true",
                    help="if the decision says so, rewrite the baseline file in place")
    args = ap.parse_args()

    with open(args.output, encoding="utf-8", errors="replace") as fh:
        text = fh.read()

    measured = parse_output(text)
    baseline = load_baseline(args.baseline)
    decision = classify(measured, args.sbt_exit, baseline)

    result = {**measured, "sbt_exit": args.sbt_exit, **decision}
    print(json.dumps(result, indent=2, ensure_ascii=False))

    b_passed = (baseline or {}).get("passed", 0)
    b_failed = (baseline or {}).get("failed", 0)
    emit_github({
        "status": decision["status"],
        "alert": str(decision["alert"]).lower(),
        "update_baseline": str(decision["update_baseline"]).lower(),
        "reason": decision["reason"],
        "passed": measured["passed"],
        "failed": measured["failed"],
        "total": measured["total"],
        "baseline_passed": b_passed,
        "baseline_failed": b_failed,
    }, "GITHUB_OUTPUT")

    emit_github({}, "GITHUB_STEP_SUMMARY")
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as fh:
            icon = {"broken": "🔴", "regression": "🚨", "seeded": "🌱",
                    "improved": "📈", "ok": "✅"}.get(decision["status"], "•")
            fh.write(f"## {icon} Test regression check — `{decision['status']}`\n\n")
            fh.write(f"- **Motivo:** {decision['reason']}\n")
            fh.write(f"- **Medido:** passed={measured['passed']}, failed={measured['failed']}, "
                     f"total={measured['total']}, suites abortadas={measured['aborted_suites']}\n")
            fh.write(f"- **Baseline:** passed={b_passed}, failed={b_failed}\n")

    if args.write_baseline and decision["update_baseline"]:
        with open(args.baseline, "w", encoding="utf-8") as fh:
            json.dump({
                "seeded": True,
                "passed": measured["passed"],
                "failed": measured["failed"],
                "total": measured["total"],
            }, fh, indent=2)
            fh.write("\n")

    return 0


if __name__ == "__main__":
    sys.exit(main())
