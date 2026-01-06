define([], function() {
'use strict';

var isFemale = function() {
	return function(nodes, currentNode) {
		return nodes.filter(function(n){return n.sex === 'Female' && n.alias !== currentNode.alias;});
	};
};

var isMale = function() {
	return function(nodes, currentNode) {
		return nodes.filter(function(n){return n.sex === 'Male' && n.alias !== currentNode.alias;});
	};
};

return {isFemale: isFemale, isMale: isMale};

});