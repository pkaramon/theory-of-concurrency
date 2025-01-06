#!/usr/bin/env python3
from pprint import pprint


stacks = {
    'a': [],
    'b': [],
    'c': [],
    'd': [],
    'e': [],
    'f' :[]
}

initial = set([
    ('a', 'd'),
    ('b', 'e'),
    ('c', 'd'),
    ('c', 'f')
])

I = set()
for (a,b) in initial:
    I.add((a,b))
    I.add((b,a))

w = "acdcfbbe"
for a in reversed(w):
    stacks[a].append(a)
    for b, stack in stacks.items():
        if (a, b) not in I and b != a:
            stack.append('*')

foata_normal = []
while any([len(s) for s in stacks.values()]):
    tops = [s[-1] for s in stacks.values() if len(s) and s[-1] !='*' ]
    for letter in tops:
        stacks[letter].pop()
        for c in stacks.keys():
            if c != letter and (letter, c) not in I:
                stacks[c].pop()

    foata_normal.append(''.join(sorted(tops)))
print(foata_normal)
