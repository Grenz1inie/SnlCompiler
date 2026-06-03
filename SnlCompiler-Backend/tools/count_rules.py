# -*- coding: utf-8 -*-
text = open(r'd:\bianyi\src\com\snl\compiler\infra\config\Constants.java', encoding='utf-8-sig').read()
import re
nts = re.findall(r'nonTerminal\.add\("([^"]+)"\)', text)
terms = re.findall(r'terminal\.add\("([^"]+)"\)', text)
for i, t in enumerate(terms):
    print(i, t)
print('---')
for i, t in enumerate(nnts := nts):
    if t in ('Exp', 'Term', 'Factor', 'RelExp', 'ActParamList', 'AssignmentRest', 'OtherTerm', 'OtherFactor'):
        print(i, t)
# show analysis rows for CHARC col
for line in text.splitlines():
    if '[37]' in line and 'analysis' in line:
        print(line.strip())
