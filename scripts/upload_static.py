#!/usr/bin/env python

from os.path import expanduser
from cStringIO import StringIO
from boto.s3.key import Key
import boto
from boto.s3.connection import S3Connection
import httplib, urllib, sys, ntpath, gzip, argparse, os

parser = argparse.ArgumentParser()

parser.add_argument("-js", "--javascript", action="store_true", help="marks file as js file")
parser.add_argument("-css", "--stylesheet", action="store_true", help="marks file as css file")
parser.add_argument("path", help="path of file to be uploaded")
args = parser.parse_args()

path = args.path

if not os.path.isabs(args.path):
    path = os.path.join(os.path.dirname(__file__), args.path)

aws_key = ""
aws_secret = ""

def upload():
    with open(expanduser("~/.cillo/prod_web.conf")) as file:
        for line in file:
            line = line.rstrip()
            if line.startswith("aws.key"):
                aws_key = line.split("=", 1)[1].replace('"', '')
            if line.startswith("aws.secret"):
                aws_secret = line.split("=", 1)[1].replace('"', '')
    if args.javascript:
        f = optimize_js()
        content_type = 'application/javascript'
        headers = {'Content-Encoding' : 'gzip'}
    else:
        f = open(path)
        content_type = None
        headers = None
    conn = S3Connection(aws_key, aws_secret)
    bucket = conn.get_bucket('cillo-static')
    key = Key(bucket)
    if args.javascript:
        key.key = 'js/' + ntpath.basename(path)        
        key.set_contents_from_filename('/tmp/' + ntpath.basename(path), headers=headers)
    elif args.stylesheet:
        key.key = 'css/' + ntpath.basename(path)        
        key.set_contents_from_filename(path, headers=headers)
    else:
        key.key = ntpath.basename(path)
        key.set_contents_from_filename(path, headers=headers)
    
def optimize_js():
    with open(path, 'rb') as f:
        d = f.read()
        params = urllib.urlencode([
            ('js_code', d),
            ('compilation_level', 'SIMPLE_OPTIMIZATIONS'),
            ('output_format', 'text'),
            ('output_info', 'compiled_code'),
        ])
        headers = { "Content-type": "application/x-www-form-urlencoded" }
        conn = httplib.HTTPConnection('closure-compiler.appspot.com')
        conn.request('POST', '/compile', params, headers)
        response = conn.getresponse()
        data = response.read()
        conn.close()
        g = gzip.open('/tmp/' + ntpath.basename(path), 'wb', 5)
        g.write(data)
        g.close()

def test():
    if args.javascript:
        print args.path
    if not args.javascript:
        print "asdf"

if __name__ == "__main__":
    upload()
