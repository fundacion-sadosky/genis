/**
 * A common directive. It would also be ok to put all directives into one file,
 * or to define one RequireJS module that references them all.
 */
define([ 'angular', 'jquery'], function(angular, $) {
	'use strict';

	var mod = angular.module('common.directives.pdgLocus', []);
	
	mod.directive('pdgLocus', [ 'profileHelper', '$log', function(pHelper/*, $log*/) {
		
		return {
			//restrict : 'E',
			scope: {
				locus: '=info',
                loci: '=',
				status: "=",
				index: "=",
                newAnalysis: "=",
				chkAnalysis: "=",
				errorMsg: "=",
				validateLocusBase: "&validateLocus",
				labels: "=",
				enableLabelsStatus: "=",
				selectedAlleles: "=",
				profile: "="
			},
			templateUrl: '/assets/javascripts/common/directives/pdg-locus.html',
			
			link : function(scope) {
				scope.locusById = scope.$parent.locusById;
                scope.isOutOfLadder = scope.$parent.isOutOfLadder;

                scope.regexPattern = pHelper.getRegExMap()[scope.locus.chromosome];
                scope.allelesList = scope.newAnalysis.genotypification[scope.locus.id];
                
				scope.addAllele = function() {
					scope.locus.alleleQty += 1;
				};
				
				scope.removeAllele = function() {
					scope.allelesList.pop();
					scope.locus.alleleQty -= 1;
				};
				
				scope.validateLocus = function(locus) {
					scope.validateLocusBase()(scope.newAnalysis, scope.chkAnalysis, locus, scope.loci);
				};

                scope.elem = "#" + scope.index + "-popover-container";

                scope.$watch('errorMsg', function(value) {
					if (!value){
                        $(scope.elem).popover('destroy'); //$('[name="' + scope.locus.id + '"]').popover('destroy');
					} else {
                        $(scope.elem).popover('destroy');
                        $(scope.elem).popover({
							content : '<div class="text-center" style="color: tomato; width: 200px; ">' + value + '</div>',
							trigger: 'manual',
							html : true,
							placement : 'right'}).popover('show');
						}
				}, 
				false);

                $(scope.elem).on('$destroy', function() {
                    $(scope.elem).popover('destroy');
				});
			}
		};
	} ]);

	return mod;
});