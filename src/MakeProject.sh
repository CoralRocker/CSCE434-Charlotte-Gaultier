#!/bin/bash

ZIPFILE=project.zip

rm -f $ZIPFILE
zip $ZIPFILE ast/* coco/* types/*