#!/usr/bin/env python3

import argparse
import json
import base64
import hmac
import struct
import sys
import time
from typing import Mapping, Optional
from unittest.mock import patch
try:
  import pytest
except ImportError:
  pass


def calculate_totp(key: str) -> str:
  n_digits: int = 6
  time_step: int = 30
  counter = struct.pack(
    ">Q",
    int((time.time() / time_step))
  )
  key_bytes = base64.b32decode(
    f"{key.upper()}{'=' * ((8 - len(key)) % 8)}"
  )
  mac = (
    hmac
    .new(key_bytes, counter, "sha1")
    .digest()
  )
  offset = mac[-1] & 0x0f
  mac = mac[offset:offset + 4]
  binary = struct.unpack('>L', mac)[0] & 0x7fffffff
  return (
    str(binary)[-n_digits:]
    .zfill(n_digits)
  )


def _check_jsonfile_content(data: list[Mapping]) -> bool:
  try:
    assert isinstance(data, list)
    assert len(data) > 0
    for user_data in data:
      assert "user" in user_data
      assert "secret" in user_data
    return True
  except AssertionError:
    return False


def validate_file(file_path: str) -> Optional[Mapping]:
  """
  Validates that a given path is a valid json file.
  """
  try:
    with open(file_path, "r", encoding="utf-8") as fin:
      data = json.load(fin)
      if not _check_jsonfile_content(data):
        return None
      result = {x["user"]: x["secret"] for x in data}
      return result
  except IOError:
    return None


def main():
  """
  Calculates the TOTP code from secret key.
  All parameters are adjusted for GENis project.
  """
  parser = argparse.ArgumentParser(
    description = "TOTP token utility script",
    usage = (
      'python3 generate_topt_token.py --json_file FILE.JSON --username USER\n\n'
      'This script generates TOTP token that can be used in GENis.\n'
      'This script is intended to be used in a development enviroment.\n'
      'Generate a token using the local machine time and a secret user key.\n'
      'The users keys are stores in a json file with the following structure:\n\n'
      '    [\n'
      '      {"user": "john_doe", "secret": "ABCDEDCBA"},\n'
      '      {"user": "jane_doe", "secret": "123454321"}\n'
      '    ]\n'
    )
  )
  parser.add_argument(
    '--json_file',
    type=str,
    required=True,
    help='The path to the json file'
  )
  parser.add_argument(
    '--username',
    type=str,
    required=True,
    help='The username'
  )
  args = parser.parse_args()
  data = validate_file(args.json_file)
  if not data:
    sys.exit(
      "Provided file is not a valid json file. "
      "Please provide a valid json file."
    )
  secret_key = data.get(args.username)
  if not secret_key:
    sys.exit(
      f"Username {args.username} is not present in json file."
    )
  print(calculate_totp(secret_key))


if __name__ == "__main__":
  main()

# Tests
# Requires pytest module.
# Run as:
#     pytest utils/generate_totp_token.py

def test_calculate_totp():
  """
  Test that calculate_totp function generates the correct token.
  The expected tokens were generated using the app https://totp.app/
  """
  mocked_time = 1688564980.342892
  with patch('time.time', return_value=mocked_time):
    assert calculate_totp("AAAAAAAA") == "767534"
  mocked_time = 1688565039.8954563
  with patch('time.time', return_value=mocked_time):
    assert calculate_totp("ABCDE") == "530376"

def test_check_jsonfile_content():
  # data should content at least "user" and "secret" fields.
  content = [
    {
      "user": "john_doe",
      "secret": "XXXXX"
    }
  ]
  # data should content both "user" and "secret" fields.
  assert _check_jsonfile_content(content)
  content = [
    {
      "secret": "XXXXX"
    }
  ]
  assert not _check_jsonfile_content(content)
  content = [
    {
      "user": "john_doe",
    }
  ]
  assert not _check_jsonfile_content(content)
  # data may have other fields
  content = [
    {
      "user": "john_doe",
      "secret": "XXXXX",
      "pass": "pass"
    }
  ]
  assert _check_jsonfile_content(content)
  # data must be a list
  content = {
      "user": "john_doe",
      "pass": "pass"
    }
  # noinspection PyTypeChecker
  assert not _check_jsonfile_content(content)
  # data should be not empty
  content = []
  assert not _check_jsonfile_content(content)
