#!/bin/bash
#
# This script copies the mobile-chrome-app plugin api tests into this desktop chrome app's tests/ folder
# References to these scripts much still be added manually to this test app (add <script> to index.html)
#

rm -rf tests
mkdir tests
cp ../plugins/*/test/* tests/
