const fs = require('fs');
const path = require('path');

const target = /color:\s*#333/g;
const replacement = 'color: "#333"';

function walk(dir) {
    let results = [];
    const list = fs.readdirSync(dir);
    list.forEach(file => {
        file = path.join(dir, file);
        const stat = fs.statSync(file);
        if (stat && stat.isDirectory()) {
            results = results.concat(walk(file));
        } else if (file.endsWith('.tsx') || file.endsWith('.ts') || file.endsWith('.js') || file.endsWith('.jsx')) {
            results.push(file);
        }
    });
    return results;
}

const files = walk('d:/Workspace/DmsUi/src');
let changedCount = 0;
files.forEach(file => {
    const content = fs.readFileSync(file, 'utf8');
    if (target.test(content)) {
        const newContent = content.replace(target, replacement);
        fs.writeFileSync(file, newContent, 'utf8');
        changedCount++;
        console.log('Fixed quotes in', file);
    }
});
console.log('Total files fixed:', changedCount);
