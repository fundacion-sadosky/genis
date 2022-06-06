define(['angular'], function(angular) {
  'use strict';
  
  var mod = angular.module('common.directives.pdgGenPopover', ['jm.i18next']);
  
  
  mod.directive('pdgGenPopover', ['$compile', function($compile) {
return {
			restrict: 'A',
			scope: {
				gen: '='
			},
			link: function(scope, el) {

				el.on('$destroy', function(/*event*/) {
					el.popover('destroy');
				});

				var helpTipScope = scope.$new(true);
				helpTipScope.gen = scope.gen;

                var table = '<table style="width:100%;padding: 0px 5px 0px 5px;" cellpadding="10"><thead><th style="padding: 0px 0px 0px 15px;" class="text-center">Marcador</th><th style="padding: 0px 15px 0px 15px;" class="text-center">Alelos</th><th style="padding: 0px 15px 0px 0px;" class="text-center">Rango</th></thead><tbody><tr ng:repeat="l in gen"  align="center"><td>{{l.locus}}</td><td class="text-center"><span class="text-center" ng:repeat="allele in l.alleles track by $index"> <span ng-class="{outofladder: l.isOutOfLadder(l,allele)}">{{allele}}</span></span></td><td class="text-center" style="padding: 0px 15px 0px 0px;">&nbsp;{{l.range}}</td></td></tr></tbody></table>';
				var helpTipContent = $compile(table)(helpTipScope);
				
				el.popover({
                    container: 'body',
					content : helpTipContent, 
					html : true,
					trigger: 'focus',
					placement : 'left'});
			}
};
}]);

  return mod;
});