define(['angular','lodash'], function(angular,_) {
  'use strict';
  
  var mod = angular.module('common.directives.pdgGenPopoverMt', ['jm.i18next']);
  
  
  mod.directive('pdgGenPopoverMt', ['$compile','profileHelper', function($compile, profileHelper) {
return {
			restrict: 'A',
			scope: {
				gen: '='
			},
			link: function(scope, el) {

                function sortNumber(a, b)
                {
                    return a - b;
                }

				el.on('$destroy', function(/*event*/) {
					el.popover('destroy');
				});

				var helpTipScope = scope.$new(true);
				if(_.isUndefined(scope.gen)){
					return;
				}
				helpTipScope.gen = scope.gen;

                var table =
					'<table style="width:100%;padding: 0px 5px 0px 5px;" cellpadding="10">' +
					'	<thead>' +
					'		<th style="padding: 0px 15px 0px 15px;" class="text-center">Rango</th>' +
					'		<th style="padding: 0px 15px 0px 0px;" class="text-center">Mutaciones</th>' +
					'	</thead><tbody>';


				var table2 = "";

				var variaciones = scope.gen.filter(function (item) {
					return !item.locus.endsWith("_RANGE");
                });

				variaciones = _.keyBy(variaciones, 'locus');

				var rango = scope.gen.filter(function (item) {
                    return item.locus.endsWith("_RANGE");
                });

                rango= _.keyBy(rango, 'locus');


				for(var i=1 ; i< 5 ; i++){
					var hv = "HV"+i;
					var hvRange= "HV"+i+"_RANGE";

					if(!_.isUndefined(variaciones[hv]) && !_.isUndefined(rango[hvRange])   ){
						rango[hvRange].alleles.sort( sortNumber );
                    table2 += '	<tr align="center"><td class="text-center"><'+rango[hvRange].alleles[0] +'-'+ rango[hvRange].alleles[1] +'> </td><td class="text-center">	';

                        for(var j=0; j<variaciones[hv].alleles.length ; j++){
                        table2 += '<span class="text-center"> '+ profileHelper.convertMt(variaciones[hv].alleles[j],hv) +' </br></span>' ;
                        }
                        table2 += '</td></tr>';
					}
                }

				table2.concat('</tbody></table>');


				var helpTipContent = $compile(table.concat(table2))(helpTipScope);
				
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