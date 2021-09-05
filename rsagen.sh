#!/bin/sh


#Private-key
openssl genrsa -out $1/privatekey.pem 2048

#Public-key
openssl rsa -pubout -in $1/privatekey.pem -out $1/publickey.pem
