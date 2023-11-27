#!/bin/bash

ZIPFILE=project.zip

rm -f $ZIPFILE
zip $ZIPFILE ast/* coco/* types/* ir/* ir/cfg/* ir/cfg/optimizations/* ir/cfg/registers/* ir/tac/* ir/cfg/CodeGen/*