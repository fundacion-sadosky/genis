define([ 'angular', 'jquery'], function(angular, $) {
	'use strict';

	var mod = angular.module('common.directives.pdgAlleleLabelPicker', []);
	
	mod.directive('pdgAlleleLabelPicker', [function() {

		return {
			restrict : 'A',
			link : function(scope, el/*, attrs*/) {
				var toggle = true;
				var locusId = scope.locus.id;
				scope.myLabels = [];

				// workaround for dna.html locus table
				if (scope.allele) { 
					el.val(scope.allele); 
					locusId = scope.locus;
				}

				scope.setLabelColor = function () {
					
					if (!angular.isObject(scope.profile) || !angular.isObject(scope.profile.labeledGenotypification)) { return; }
					
					scope.myLabels = [];
					
					for (var i = 1; i < 5; i++) { el.removeClass('mixes-label-' + i); }
					
					Object.keys(scope.profile.labeledGenotypification).forEach(
						function(lbl){
							var g = scope.profile.labeledGenotypification[lbl];
							Object.keys(g).forEach(
								function(locus) {
									if (locus === locusId &&
										(g[locus].map(function(x){return x.toString();})).indexOf(el.val()) > -1) {
										if (scope.labels[lbl]) {
											el.addClass('mixes-label-' + scope.labels[lbl].id);
											scope.myLabels.push(lbl);
										}
									}
							});
						});
				};

				el.bind('focus', function() {
                    if (el.offset().top > 500) {
                        $('html, body').animate({
                            scrollTop: el.offset().top - 400
                        }, 200);
                    }
				});

				el.bind('click', function() {
					if (scope.isMixture && el.val().toString().length > 0 && scope.enableLabelsStatus) {
						
						var element = {"locusId" : locusId, "alleleValue":el.val()};

						var indexOfElement = -1;
							
						scope.selectedAlleles.forEach(function(arrayElement, index) {
							if (arrayElement.locusId === element.locusId && 
									arrayElement.alleleValue === element.alleleValue) {
									indexOfElement = index;
								}
							}
						);
						
						if (indexOfElement >=0) {
							scope.selectedAlleles.splice(indexOfElement,1);
							el.removeClass('mixes-selected');//scope.selected = false;
							
						} else {
							scope.selectedAlleles.push(element);
							el.addClass('mixes-selected');//scope.selected = true;
						}
						toggle = !toggle;
					}
				});
				
				scope.$watch('profile.labeledGenotypification', function() { 
					scope.setLabelColor();
					el.removeClass('mixes-selected');
				}, true);

                scope.$watch('allelesList', function() {
                    scope.setLabelColor();
                    el.removeClass('mixes-selected');
                }, true);
				
				scope.setLabelColor();

			}
		
		};
		
	} ]);

	return mod;
});