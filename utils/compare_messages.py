#!/usr/bin/env python3
# pylint: disable=missing-module-docstring

import argparse
import sys
from typing import Mapping, Optional, Dict
from enum import Enum
import re

# pylint: disable=missing-module-docstring
# pylint: disable=missing-function-docstring
# pylint: disable=bad-indentation

def load_messages(file: str) -> Mapping:
  line_pattern = r"(^[^=]+)=(.+)$"
  result = {}
  with open(file, "r", encoding="utf-8") as fin:
    for line in fin:
      line = line.strip()
      if not line or line.startswith("#"):
        continue
      if m := re.match(line_pattern, line):
        result[m.group(1)] = m.group(2)
  return result

class Action(Enum):
  """A class to represent actions"""
  COMPARE_SAME_LANG = 1
  COMPARE_DIFF_LANG = 2
  MERGE = 3

def validate_action(argument: str) -> Optional[Action]:
  if argument.lower() == "merge":
    return Action.MERGE
  if argument.lower() == "compare-same-language":
    return Action.COMPARE_SAME_LANG
  if argument.lower() == "compare-diff-language":
    return Action.COMPARE_DIFF_LANG
  return None

def compare_same_language(messages1:Mapping, messages2: Mapping) -> Dict:
  keys1 = set(messages1.keys())
  keys2 = set(messages2.keys())
  results = {
    "not_in_messages1" : [],
    "not_in_messages2" : [],
    "different_values" : [],
  }
  for k in keys1.union(keys2):
    if k not in keys1:
      results["not_in_messages1"].append(k)
    if k not in keys2:
      results["not_in_messages2"].append(k)
    if k in keys1 and k in keys2:
      value1 = messages1[k]
      value2 = messages2[k]
      if value1 != value2:
        results["different_values"].append(k)
  return results

def compare_diff_language(messages1: Mapping, messages2: Mapping) -> Dict:
  keys1 = set(messages1.keys())
  keys2 = set(messages2.keys())
  results = {
    "not_in_messages1" : [],
    "not_in_messages2" : [],
  }
  for k in keys1.union(keys2):
    if k not in keys1:
      results["not_in_messages1"].append(k)
    if k not in keys2:
      results["not_in_messages2"].append(k)
  return results

def merge(messages1:Mapping, messages2:Mapping) -> Mapping:
  keys1 = set(messages1.keys())
  keys2 = set(messages2.keys())
  results = {}
  for k in keys1.union(keys2):
    if k not in keys1:
      results[k] = messages2[k]
    if k not in keys2:
      results[k] = messages1[k]
    if k in keys1 and k in keys2:
      results[k] = messages1[k]
  return results

def print_merge_results(results:Mapping):
  for k in sorted(results.keys()):
    print(f"{k}={results[k]}")

def print_compare_results(results:Mapping):
  results_types = sorted(results.keys())
  titles = {
    "not_in_messages1": "Keys not found in messages file 1:",
    "not_in_messages2": "Keys not found in messages file 2:",
    "different_values": "Keys with different values:"
  }
  for r_type in results_types:
    print(titles[r_type])
    for line in sorted(results[r_type]):
      print(f"[{line}]")

def main():
  """
  Compare two json files of translations.
  """
  parser = argparse.ArgumentParser(
    description = "Json translations comparison tool",
    usage = (
      'python3 generate_topt_token.py --action ACTION '
      '--messages1 FILE --messages2 FILE\n\n'
      'Actions allowed are compare-same-language, compare-diff-language and merge.\n'
      '  - compare-same-language is intended to use to compare two version of the \n'
      '    same translation file in the same language. Checks for absences of keys \n'
      '    in both files, and for differences of values.\n'
      '  - compare-diff-language is intended to use to compare two version of the \n'
      '    same translation file in different languages. Checks for absences of keys.\n'
      '    assumes that values are always different\n'
      '  - merge is intended to use to merge two version of the \n'
      '    same translation file in the same language. Takes the keys from both files.\n'
      '    taking file 1 precedence over file 2 when keys is present in both files.\n'
    )
  )
  parser.add_argument(
    '--action',
    type=str,
    required=True,
    help='What to do with the json files'
  )
  parser.add_argument(
    '--messages1',
    type=str,
    required=True,
    help='The path to the first messages file'
  )
  parser.add_argument(
    '--messages2',
    type=str,
    required=True,
    help='The path to the second messages file'
  )
  args = parser.parse_args()
  action = validate_action(args.action)
  if not action:
    sys.exit("We need an action to do something.")
  json1 = load_messages(args.messages1)
  json2 = load_messages(args.messages2)
  if not json1 or not json2:
    sys.exit(
      "Provided file is not a valid json file. "
      "Please provide a valid json file."
    )
  if action == Action.COMPARE_SAME_LANG:
    print("Running compare same")
    results = compare_same_language(json1, json2)
    print_compare_results(results)
  if action == Action.COMPARE_DIFF_LANG:
    print("Running compare diff")
    results = compare_diff_language(json1, json2)
    print_compare_results(results)
  if action == Action.MERGE:
    print("Running merge")
    results = merge(json1, json2)
    print_merge_results(results)

if __name__ == "__main__":
  main()

# Tests
# Requires pytest module.
# Run as:
#     pytest utils/generate_totp_token.py
def test_compare_json_with_identical_data():
  messages1 = {
    "key1": "value1"
  }
  messages2 = {
    "key1" : "value1"
  }
  results = compare_same_language(messages1, messages2)
  assert len(results["not_in_messages1"]) == 0
  assert len(results["not_in_messages2"]) == 0
  assert len(results["different_values"]) == 0

def test_compare_json_with_different_data_1():
  messages1 = {
    "key1": "value1"
  }
  messages2 = {
    "key1" : "value2",
  }
  results = compare_same_language(messages1, messages2)
  assert len(results["not_in_messages1"]) == 0
  assert len(results["not_in_messages2"]) == 0
  assert len(results["different_values"]) == 1
  assert results["different_values"][0] == "key1"

def test_compare_json_with_different_data_2():
  messages1 = {
    "key1" : "value1",
  }
  messages2 = {
    "key2" : "value2",
  }
  results = compare_same_language(messages1, messages2)
  assert len(results["not_in_messages1"]) == 1
  assert results["not_in_messages1"][0] == "key2"
  assert len(results["not_in_messages2"]) == 1
  assert results["not_in_messages2"][0] == "key1"
  assert len(results["different_values"]) == 0

def test_merge_case_1():
  messages1 = {
    "key1": "value1",
    "key2": "value2",
    "key4": "value4a"
  }
  messages2 = {
    "key1": "value1",
    "key3": "value3",
    "key4": "value4b"
  }
  expected = {
    "key1": "value1",
    "key2": "value2",
    "key3": "value3",
    "key4": "value4a",
  }
  result = merge(messages1, messages2)
  assert result == expected
