APDU Specification
==================

Terminal -> Smartcard (Incoming)
Standard command APDU format

1. CLA, 1 byte
   Identifies a class of instructions
   Default 00
2. INS, 1 byte
   Identifies a given instruction (should be an even number)
3. P1:P2, 2 bytes
   P1 and P2 describe the options for a command
   Often, we use P2 as a message counter
4. Lc, 1 byte
   The length of the incoming data
5. Incoming data, between 0-255 bytes
6. Le, 1 byte (optional)
   Expected length of the outgoing data

Smartcard -> Terminal (Outgoing)
Standard response APDU format

1. Lr, 1 byte, (optional)
   Actual length of the outgoing data
2. Outgoing data, between 0-256 bytes
3. SW1:SW2, 2 bytes (optional)
   SW1 and SW2 is a status word, see ISO7816
   SW1:SW2 == 90:00 is default for NO_ERROR


Length of crypto primitives
---------------------------

RSAPublicKey: 162 bytes
RSAPrivateKey: 634 bytes
Signature (no data): 128 bytes
Certificate: 1+162+128 = 291 bytes (plus 2 extra bytes for the exp date of a smartcard)
Nonce: 16 bytes


Personalizing the smartcard
------------------------

PT -> S: SK_S, VK_CA, cert_S

Upload $SK_S$ (634 bytes), $VK_{CA}$ (162 bytes) and $cert_S$ (293 bytes) 
to the smartcard, total 1089 bytes. Consists of 5 messages (because there is
too much data)

Incoming:
CLA     00
INS     10
P1      00
P2      00, 01, 02, 03, 04
Lc      FF, FF, FF, FF, 45 
Data

Outgoing:
SW1     63, 63, 63, 63, 90
SW2     10, 10, 10, 10, 00

The comma notation should be read going left-to right in time. So here the
order of messages will be:
  In:  00:10:00:00:FF:<data>
  Out: 63:10
  In:  00:10:00:01:FF:<data>
  Out: 63:10
  In:  00:10:00:02:FF:<data>
  Out: 63:10
  In:  00:10:00:03:FF:<data>
  Out: 63:10
  In:  00:10:00:04:45:<data>
  Out: 90:00


6.4 Mutual authentication of RT and S
-------------------------------------

1. RT -> S: cert_RT, N

Send $cert_{RT}$ (291 bytes) and $N$ (16 bytes) to intialize mutual
authentication (total 307 bytes)

Incoming:
CLA     00
INS     20
P1      00
P2      00, 01
Lc      FF, 34
Data

Outgoing:
SW1     90
SW2     00


2. S -> RT: {| cert_S, {<N, K_tmp>}SK_S |}EK_RT

After the last data has been sent, send back the response (note that the
terminal needs to request for more responses).
TODO: find out the size of the response

Incoming:
CLA     00
INS     22
P1      00
P2      00, 01, ..

Outgoing:
Lr      FF, FF, .., ??
Data
SW1     63, 63, .., 90
SW2     10, 10, .., 00


4. RT -> S: { K_RT,S } K_tmp

Send the session key K_RT,S, encrypted with the temporary key K_tmp
TODO: length of message

Incoming:
CLA     00
INS     24
P1      00
P2      00 
Lc      ??
Data    Encrypted session key

Outgoing:
SW1     90
SW2     00


6.5 Mutual authentication of VT and S
-------------------------------------

1. VT -> S: cert_VT, N

Send $cert_{VT}$ (291 bytes) and $N$ (16 bytes) to intialize mutual
authentication (total 307 bytes)

Incoming:
CLA     00
INS     30
P1      00
P2      00, 01
Lc      FF, 34
Data

Outgoing:
SW1     90
SW2     00


2. S -> VT: {| cert_S, {<N, K_tmp>}SK_S |}EK_VT

After the last data has been sent, send back the response (note that the
terminal needs to request for more responses).
TODO: find out the size of the response

Incoming:
CLA     00
INS     32
P1      00
P2      00, 1, ..

Outgoing:
Lr      FF, FF, .., ??
Data
SW1     63, 63, .., 90
SW2     10, 10, .., 00


4. VT -> S: { K_VT,S } K_tmp

Send the session key K_VT,S, encrypted with the temporary key K_tmp
TODO: length of message

Incoming:
CLA     00
INS     34
P1      00
P2      00 
Lc      ??
Data

Outgoing:
SW1     90
SW2     00


6.6 Vehicle ignition
--------------------

2. VT -> S: {"ignition"}K_VT,S
5: S -> VT: { {<"ignition ok", km>}SK_S }K_VT,S

Send the command for ignition, confirm the ignition
TODO: length of encrypted messages
TODO: should 5a. respond with a 9000?

Incoming:
CLA     00
INS     40
P1      00
P2      00
Lc      ??
Data

Outgoing:
Lr      ??
Data
SW1     90
SW2     00

5a. S -> VT: { {<"not enough km">} }K_VT,S

Outgoing:
Lr      ??
Data
SW1     90
SW2     00


6.7 Driving
-----------

1. VT -> S: {"deduct km"}K_VT,S
6. S -> VT: { {<"deduct km ok", km>}SK_S }K_VT,S

Send the command for driving one km, confirm deduction
TODO: length of messages

Incoming:
CLA     00
INS     42
P1      00
P2      00
Lc      ??
Data

Outgoing:
Lr      ??
Data
SW1     90
SW2     00

5a. S -> VT: { {<"deduct km failed: no balance">}SK_S }K_VT,S

Outgoing:
Lr      ??
Data
SW1     90
SW2     00

Message 6b is implicit and encompasses all responses where SW1:SW2 != 90:00


6.8 Stopping
------------

2. VT -> S: {"stop"}K_VT,S
6. S -> VT: {<"stop ok">}SK_S
TODO: length of messages

Incoming:
CLA     00
INS     44
P1      00
P2      00
Lc      ??
Data

Outgoing:
Lr      ??
Data
SW1     90
SW2     00


6.9 Renewing certificate
------------------------

4. RT -> S: cert_S
TODO: length

Incoming:
CLA     00
INS     50
P1      00
P2      (counter)
Lc      (???)
Data

Outgoing:
SW1     63, .., 90
SW2     10, .., 00


6.10 Topup
----------

3. S -> RT: { {<km>}SK_S }K_RT,S
TODO: length

Incoming:
CLA     00
INS     52
P1      00
P2      00

Outgoing:
Lr      ??
Data
SW1     90
SW2     00


7. RT -> S: {"topup km to", amount}K_RT,S
8. S -> RT: { {<"topup ok">, amount>}SK_S }K_RT,S
TODO: length

Incoming:
CLA     00
INS     54
P1      00
P2      00
Lc      ??
Data

Outgoing:
Lr      ??
Data
SW1     63, .., 90
SW2     10, .., 00


6.11 Refund
-----------

7. RT -> S: {"refund"}K_RT,S
9. S -> RT: { {<"refund ok">}SK_S, }K_RT,S
TODO: length

Incoming:
CLA     00
INS     56
P1      00
P2      00
Lc      ??
Data

Outgoing
Lr      ??
Data
SW1     63, .., 90
SW2     10, .., 00

