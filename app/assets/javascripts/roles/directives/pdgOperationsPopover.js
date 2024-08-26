define([], function() {
'use strict';

function pdgOperationsPopover($compile, $filter) {

	return {
		restrict: 'A',
		scope: {
			operations: '='
		},
		link: function(scope, el) {

			el.on('$destroy', function(/*event*/) {
				el.popover('destroy');
			});

			scope.translate = function(operation) {
				return $filter('i18next')('resource.' + operation.descriptionKey);
			};

			var table = '<div><table style="width:100%;">'  ;
			table += '<thead><tr style="background-color: white;"><th class="col-md-6 text-center">Descripción</th></tr></thead>';
			table += '<tbody><tr ng:repeat="o in operations | orderBy: translate">';
			table += '<td class="text-center">{{translate(o)}}</td>';
			table += '</tr></tbody></table></div>';

			var htmlTemplate = $compile(table)(scope);
			
			el.popover({
				content : htmlTemplate, 
				html : true,
				trigger: 'click',
				placement : 'left'});
		}
	};
}

return pdgOperationsPopover;

});
