# -*- coding: utf-8 -*-
"""
Created on Sun Apr 26 20:52:45 2020

@author: mavch
"""

def generateLayout():
    for i in range(9):
        r = i//3
        c = i%3
        msg = '<cell name="{:}"><x_val>{:}</x_val><y_val>{:}</y_val></cell>'
        print(msg.format(i, 25+25*c, 5+25*r))
        
def generateCards():
    msg1 = '<card><name>AS</name><value>1</value><color>black</color><suit>s</suit></card>'
    msg2 = '<card><name>AH</name><value>1</value><color>red</color><suit>h</suit></card>'
    for i in range(3):
        print(msg1)
        print(msg2)
        
def generateCells():
    msg = '<cell name="{:}"><fan>none</fan><rotation>0</rotation><init_cards><card>B,U</card></init_cards></cell>'
    for i in range(16):
        print(msg.format(i))
    print('<cell name="update"><fan>none</fan><rotation>0</rotation><init_cards><card>update,U</card></init_cards></cell>')
    
generateLayout()