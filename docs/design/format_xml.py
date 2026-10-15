"""Format only this change's Tarot XML; leave existing project layouts untouched."""
from pathlib import Path
from xml.dom import minidom
from xml.sax.saxutils import quoteattr, escape

ROOT = Path(__file__).resolve().parents[2] / 'app/src/main/res'

def render(node, depth=0):
    indent = '    ' * depth
    if node.nodeType == node.COMMENT_NODE:
        return indent + '<!--' + node.data + '-->'
    if node.nodeType != node.ELEMENT_NODE:
        return ''
    attrs = [(node.attributes.item(i).name, node.attributes.item(i).value) for i in range(node.attributes.length)]
    children = [c for c in node.childNodes if c.nodeType == c.ELEMENT_NODE or c.nodeType == c.COMMENT_NODE or (c.nodeType == c.TEXT_NODE and c.data.strip())]
    start = indent + '<' + node.tagName
    if len(attrs) <= 1:
        start += ''.join(' ' + name + '=' + quoteattr(value) for name, value in attrs)
    else:
        start += '\n' + '\n'.join(indent + '    ' + name + '=' + quoteattr(value) for name, value in attrs)
    if not children:
        return start + ' />'
    if all(c.nodeType == c.TEXT_NODE for c in children):
        return start + '>' + escape(''.join(c.data for c in children)) + '</' + node.tagName + '>'
    return start + '>\n' + '\n'.join(render(c, depth + 1) for c in children) + '\n' + indent + '</' + node.tagName + '>'

paths = [p for folder in ['layout', 'drawable', 'color', 'values'] for p in (ROOT / folder).glob('*.xml')
         if 'tarot' in p.stem]
for path in paths:
    document = minidom.parse(str(path))
    path.write_text('<?xml version="1.0" encoding="utf-8"?>\n' + render(document.documentElement) + '\n')
print('Formatted', len(paths), 'Tarot XML files')
