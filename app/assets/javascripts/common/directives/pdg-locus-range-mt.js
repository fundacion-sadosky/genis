/**
 * A common directive. It would also be ok to put all directives into one file,
 * or to define one RequireJS module that references them all.
 */
define([ 'angular', 'jquery'], function(angular, $) {
    'use strict';

    var mod = angular.module('common.directives.pdgLocusRangeMt', []);

    mod.directive('pdgLocusRangeMt', [ 'profileHelper', '$log', function(pHelper/*, $log*/) {

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
            templateUrl: '/assets/javascripts/common/directives/pdg-locus-range-mt.html',

            link : function(scope) {
                scope.locusById = scope.$parent.locusById;
                scope.isOutOfLadder = scope.$parent.isOutOfLadder;
                if(!angular.isDefined(scope.locus)){
                    return;
                }
                scope.isEven = function isEven(n) {
                    return n % 2 === 0;
                };
                scope.regexPattern = pHelper.getRegExMap()[scope.locus.chromosome];
                scope.allelesList = scope.newAnalysis.genotypification[scope.locus.id];
                if(scope.locus.id.endsWith("RANGE")){
                    scope.indexCh = "indexCh";
                    scope.regexPattern = '^[0-9]+$';
                    scope.isRange = true;
                }else{
                    scope.indexCh = "index";
                    scope.isRange = false;
                }
                scope.addAllele = function() {
                    scope.locus.alleleQty += 1;
                };

                scope.removeAllele = function() {
                    scope.allelesList.pop();
                    scope.locus.alleleQty -= 1;
                };

                scope.validateLocus = function(locus) {
                    scope.validateLocusBase()(scope.newAnalysis, scope.chkAnalysis, locus, scope.loci,scope.indexCh);
                };

                scope.elem = "#" + scope.locus.id + "-popover-container";
                scope.hidePopOver = function(){
                    console.log("hidePopover");
                    $(scope.elem).popover('destroy');
                };
                scope.$watch('errorMsg', function(value) {
                        if (!value){
                            $(scope.elem).popover('destroy'); //$('[name="' + scope.locus.id + '"]').popover('destroy');
                        } else {
                            $(scope.elem).popover({
                                content : '<div id=pop'+scope.locus.id+' style="color: tomato; width: 200px; ">'+value+'</div>',
                                trigger: 'manual',
                                html : true,
                                placement : 'top'}).popover('show');
                            $('#pop'+scope.locus.id).text(value);

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