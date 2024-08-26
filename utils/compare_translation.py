#!/usr/bin/env python3
# pylint: disable=missing-module-docstring

import argparse
import sys
import json
from typing import Mapping, Optional, List, Dict
from enum import Enum
from warnings import warn

# pylint: disable=missing-module-docstring
# pylint: disable=missing-function-docstring
# pylint: disable=bad-indentation

def load_json_file(file: str) -> Mapping:
  with open(file, "r", encoding="utf-8") as fin:
    data = json.load(fin)
    return data

class Action(Enum):
  """A class to represent actions"""
  COMPARE_SAME_LANG = 1
  COMPARE_DIFF_LANG = 1
  MERGE = 2

def validate_action(argument: str) -> Optional[Action]:
  if argument.lower() == "merge":
    return Action.MERGE
  if argument.lower() == "compare-same-language":
    return Action.COMPARE_SAME_LANG
  if argument.lower() == "compare-diff-language":
    return Action.COMPARE_DIFF_LANG
  return None

def compare_same_language(json1: Mapping, json2: Mapping) -> Dict:
  def _compare(json1: Mapping, json2:Mapping, prefix:List[str], results: Dict):
    keys1 = set(json1.keys())
    keys2 = set(json2.keys())
    for k in keys1.union(keys2):
      full_key_name = f"{'.'.join(prefix+[k])}"
      if k not in keys1:
        results["not_in_json1"].append(full_key_name)
      if k not in keys2:
        results["not_in_json2"].append(full_key_name)
      if k in keys1 and k in keys2:
        value1 = json1[k]
        value2 = json2[k]
        if isinstance(value1, str) and isinstance(value2, str):
          if value1 != value2:
            results["different_values"].append(full_key_name)
        elif isinstance(value1, Dict) and isinstance(value2, Dict):
          _compare(json1[k], json2[k], prefix+[k], results)
        else:
          results["different_types"].append(full_key_name)
  results = {
      "not_in_json1" : [],
      "not_in_json2" : [],
      "different_values" : [],
      "different_types": []
  }
  _compare(json1, json2, [], results)
  return results

def compare_diff_language(json1: Mapping, json2: Mapping) -> Dict:
  def _compare(json1: Mapping, json2:Mapping, prefix:List[str], results: Dict):
    keys1 = set(json1.keys())
    keys2 = set(json2.keys())
    for k in keys1.union(keys2):
      full_key_name = f"{'.'.join(prefix+[k])}"
      if k not in keys1:
        results["not_in_json1"].append(full_key_name)
      if k not in keys2:
        results["not_in_json2"].append(full_key_name)
      if k in keys1 and k in keys2:
        value1 = json1[k]
        value2 = json2[k]
        if isinstance(value1, Dict) and isinstance(value2, Dict):
          _compare(json1[k], json2[k], prefix+[k], results)
        else:
          results["different_types"].append(full_key_name)
  results = {
      "not_in_json1" : [],
      "not_in_json2" : [],
      "different_types": []
  }
  _compare(json1, json2, [], results)
  return results

def merge(json1:Mapping, json2:Mapping) -> Mapping:
  def _merge(json1: Mapping, json2:Mapping, prefix:List[str], results: Dict):
    keys1 = set(json1.keys())
    keys2 = set(json2.keys())
    for k in keys1.union(keys2):
      if k not in keys1:
        results[k] = json2[k]
      if k not in keys2:
        results[k] = json1[k]
      if k in keys1 and k in keys2:
        value1 = json1[k]
        value2 = json2[k]
        if isinstance(value1, str) and isinstance(value2, str):
          results[k] = json1[k]
        elif isinstance(value1, Dict) and isinstance(value2, Dict):
          results[k] = {}
          _merge(json1[k], json2[k], prefix+[k], results[k])
        else:
          warn(f"Key {k}, has different types in both json files.")
  results = {}
  _merge(json1, json2, [], results)
  return results

def print_compare_results(results: Mapping):
  results_types = sorted(results.keys())
  titles = {
    "not_in_json1": "Keys not found in json file 1:",
    "not_in_json2": "Keys not found in json file 2:",
    "different_values": "Keys with different values:",
    "different_types": "Keys with different types:"
  }
  for r_type in results_types:
    print(titles[r_type])
    for line in sorted(results[r_type]):
      print(line)

def main():
  """
  Compare two json files of translations.
  """
  parser = argparse.ArgumentParser(
    description = "Json translations comparison tool",
    usage = (
      'python3 generate_topt_token.py --action ACTION '
      '--json_file_1 FILE.JSON --json_file_2 FILE.JSON\n\n'
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
    '--json_file_1',
    type=str,
    required=True,
    help='The path to the first json file'
  )
  parser.add_argument(
    '--json_file_2',
    type=str,
    required=True,
    help='The path to the second json file'
  )
  args = parser.parse_args()
  action = validate_action(args.action)
  if not action:
    sys.exit("We need an action to do something.")
  json1 = load_json_file(args.json_file_1)
  json2 = load_json_file(args.json_file_2)
  if not json1 or not json2:
    sys.exit(
      "Provided file is not a valid json file. "
      "Please provide a valid json file."
    )
  if action == Action.COMPARE_SAME_LANG:
    results = compare_same_language(json1, json2)
    print_compare_results(results)
  if action == Action.COMPARE_DIFF_LANG:
    results = compare_diff_language(json1, json2)
    print_compare_results(results)
  if action == Action.MERGE:
    results = merge(json1, json2)
    print(json.dumps(results, indent = 2, ensure_ascii=False))

if __name__ == "__main__":
  main()

# Tests
# Requires pytest module.
# Run as:
#     pytest utils/generate_totp_token.py
def test_compare_json_with_identical_data():
  json1 = {
    "key1" : "value1",
    "key2" : {
      "key3": "value2",
      "key4": "value3",
    }
  }
  json2 = {
    "key1" : "value1",
    "key2" : {
      "key3": "value2",
      "key4": "value3",
    }
  }
  results = compare_same_language(json1, json2)
  assert len(results["not_in_json1"]) == 0
  assert len(results["not_in_json2"]) == 0
  assert len(results["different_values"]) == 0
  assert len(results["different_types"]) == 0

def test_compare_json_with_different_data_1():
  json1 = {
    "key1" : "value1",
  }
  json2 = {
    "key1" : "value2",
  }
  results = compare_same_language(json1, json2)
  assert len(results["not_in_json1"]) == 0
  assert len(results["not_in_json2"]) == 0
  assert len(results["different_values"]) == 1
  assert results["different_values"][0] == "key1"
  assert len(results["different_types"]) == 0

def test_compare_json_with_different_data_2():
  json1 = {
    "key1" : "value1",
  }
  json2 = {
    "key2" : "value2",
  }
  results = compare_same_language(json1, json2)
  assert len(results["not_in_json1"]) == 1
  assert results["not_in_json1"][0] == "key2"
  assert len(results["not_in_json2"]) == 1
  assert results["not_in_json2"][0] == "key1"
  assert len(results["different_values"]) == 0
  assert len(results["different_types"]) == 0

def test_compare_json_with_different_data_3():
  json1 = {
    "key1" : "value1",
  }
  json2 = {
    "key1" : {
      "key2" : "value1"
    }
  }
  results = compare_same_language(json1, json2)
  assert len(results["not_in_json1"]) == 0
  assert len(results["not_in_json2"]) == 0
  assert len(results["different_values"]) == 0
  assert len(results["different_types"]) == 1
  assert results["different_types"][0] == "key1"

def test_compare_json_with_different_data_4():
  json1 = {
    "key1" : {
      "key2" : "value2",
      "key3" : "value3"
    }
  }
  json2 = {
    "key1" : {
      "key2" : "value1"
    }
  }
  results = compare_same_language(json1, json2)
  assert len(results["not_in_json1"]) == 0
  assert len(results["not_in_json2"]) == 1
  assert results["not_in_json2"][0] == "key1.key3"
  assert len(results["different_values"]) == 1
  assert results["different_values"][0] == "key1.key2"
  assert len(results["different_types"]) == 0

def test_merge_case_1():
  json1 = {
    "key1" : {
      "key2": "value2",
      "key4": "value4a",
    }
  }
  json2 = {
    "key1" : {
      "key3": "value3",
      "key4": "value4b",
    }
  }
  expected = {
    "key1" : {
      "key2": "value2",
      "key3": "value3",
      "key4": "value4a",
    }
  }
  result = merge(json1, json2)
  assert result == expected
