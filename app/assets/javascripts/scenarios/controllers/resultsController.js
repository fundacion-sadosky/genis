define(['jquery','lodash'], function($,_) {
	'use strict';
	function ResultsController($scope, $modal, alertService, scenarioService, $route, $location, $filter) {

        $scope.isProcessing = false;

        $scope.printHypothesis = function(hypothesis) {
            return scenarioService.printHypothesis($scope.scenario[hypothesis], $scope.profileData);
        };

        $scope.save = function(){
            if (!$scope.scenarioId){
                $scope.openDialogForName(false, $scope.isRestricted);
            } else {
                $scope.fillScenario();
                $scope.scenarioData.scenario = $scope.scenario;
                $scope.scenarioData.result = $scope.result;
                scenarioService.update($scope.scenarioData).then(function() {
                        alertService.success({message: 'El escenario fue actualizado con éxito'});
                    },
                    function(response) {
                        alertService.error({message: response.data});
                    }
                );
            }
        };

        $scope.validate = function(confirmRes){
            if (!confirmRes) {return;}

            if (!$scope.scenarioId){
                $scope.openDialogForName(true);
            } else {
                $scope.fillScenario();
                $scope.scenarioData.scenario = $scope.scenario;
                $scope.scenarioData.result = $scope.result;
                scenarioService.validate($scope.scenarioData).then(function() {
                        alertService.success({message: 'El escenario fue validado con éxito'});
                        $route.reload();
                    },
                    function(response) {
                        alertService.error({message: response.data.message});
                    }
                );
            }
        };

        $scope.openDialogForName = function(validated, restricted) {
            $modal.open({
                templateUrl:'/assets/javascripts/scenarios/views/save-modal.html',
                controller : 'saveModalController',
                resolve: {
                    scenario: function() {
                        $scope.fillScenario();
                        return $scope.scenario;
                    },
                    results: function(){
                        return $scope.result;
                    },
                    validated: function(){
                        return validated;
                    },
                    restricted: function() {
                        return restricted;
                    }
                }
            }).result.then(function(response){
                if(!_.isUndefined(response)){
                    $location.url('/scenarios/scenario.html').search({ s:response });
                }
            });
        };
        
        $scope.getNCorrection = function(bigN) {
            $scope.isProcessing = true;
            scenarioService.getNCorrection($scope.profile, $scope.options[0].globalCode, 
                                           bigN, $scope.result.total).then(function(response) {
                $scope.isProcessing = false;
                $scope.nCorrection = response.data;
            }, function(response) {
                $scope.isProcessing = false;
                alertService.error({ message: response.data });
                $scope.nCorrection = undefined;
            });
        };
        
        $scope.showNCorrection = function() {
            var unvalidated = !$scope.scenarioData || $scope.scenarioData.state.toString() !== 'Validated';
            var onlyOneOption = $scope.options.length === 1;
            var allOneContributor = $scope.profileData[$scope.profile].contributors === 1 &&
                                    $scope.profileData[$scope.options[0].globalCode].contributors === 1;
            return unvalidated && onlyOneOption && allOneContributor && $scope.result.total > 1;
        };

        $scope.printReport = function() {
            var head = '<head><title>Resultados ' + $scope.profile + '</title>';
            $("link").each(function () {
                head += '<link rel="stylesheet" href="' + $(this)[0].href + '" />';
            });

            head += "</head>";

            var beginResult = '<div class="options-header scenarios-section"><div class="panel-body padding-left-0 padding-right-0"><div class="col-md-12" style="text-align: center">';
            var lrResult = $('#divLRResult').html();
            var endResult = '</div></div></div>';

            var correctionResult = '';
            if ($scope.nCorrection) {
                var beginCorrectionResult = '<div class="col-md-6 m-t-10">';
                var nCorrection = $('#divNCorrection').clone();
                $(nCorrection).children('#spanN').remove();
                nCorrection = $(nCorrection).prepend('<div class="m-r-10"><span class="bold">Cantidad de perfiles evaluados: </span>' + $scope.nCorrection.n + '</div>').prepend('<div class="m-r-10"><span class="bold">Cantidad de individuos en la población de interés: </span>' + $('#bigN-input').val() + '</div>').html();
                var endCorrectionResult = '</div>';
                correctionResult = beginCorrectionResult + nCorrection + endCorrectionResult;
            }

            var beginDetail = '<div class="row m-l-10 m-r-10 m-t-10">';
            var beginPartialResult = '<div class="scenarios-section" style="padding-bottom: 25px;">';
            var partialResult = $('#divPartialResult').html();
            var endPartialResult = '</div>';
            var beginScenario = '<div class="scenarios-section" style="page-break-after:always;">';
            var scenario = $('#divScenario').html();
            var endScenario = '</div>';
            var endDetail = '</div>';
            var detail = beginDetail + beginScenario + scenario + endScenario + beginPartialResult + partialResult + endPartialResult + endDetail;

            var beginGenotypification = '<div class="scenarios-section" style="page-break-after:always;"><h3 class="m-b-10">Comparación de perfiles</h3>';
            var otherProfilesHead = '';
            $scope.profiles.forEach(function(p){
                otherProfilesHead +=
                    '<td id="td-comp-gen-3" class="text-center column col-md-2" title="Ver información de la causa">' +
                        '<div class="comparison-profileID m-b-10">' +
                            $filter('prittyLimitTo')($filter('showcode')($scope.profileData[p]), 20, false) +
                        '</div>' +
                    '</td>';

            });

            var beginTableGenotypification =
                '<table>' +
                    '<thead>' +
                        '<tr>' +
                            '<td id="td-comp-gen-1" class="column col-md-1"></td>' +
                            '<td id="td-comp-gen-2" class="text-center column col-md-3" title="Ver información de la causa">' +
                                '<div class="comparison-profileID m-b-10">' +
                                    $filter('prittyLimitTo')($filter('showcode')($scope.profileData[$scope.profile]), 20, false) +
                                '</div>' +
                            '</td>' +
                            otherProfilesHead +
                         '</tr>' +
                    '</thead>';

            var bodyTableGenotypification = '<tbody>';
            $scope.comparison.forEach(function(item){
                var profileAlleles = '';
                if (item.g[$scope.profile]) {
                    item.g[$scope.profile].forEach(function (allele) {
                        profileAlleles +=
                            '<span class="brick-allele"><span class="text-center comparision-allele">' +
                            allele +
                            '</span></span>';
                    });
                }

                var otherProfilesAlleles = '';
                $scope.profiles.forEach(function(p){
                    var otherAlleles = '';
                    if (item.g[p]) {
                        item.g[p].forEach(function (allele) {
                            otherAlleles +=
                                '<span class="text-center comparision-allele">' +
                                allele +
                                '</span>';
                        });
                    }

                    otherProfilesAlleles += '<td class="text-center">' + otherAlleles + '</td>';
                });

                bodyTableGenotypification +=
                    '<tr>' +
                        '<td>' +
                            item.locus +
                        '</td>' +
                        '<td class="text-center">' +
                        profileAlleles +
                        '</td>' +
                        otherProfilesAlleles +
                        '<td></td>' +
                    '</tr>';
            });

            bodyTableGenotypification += '</tbody>';
            var endTableGenotypification = '</table>';
            var endGenotypification = '</div>';

            var genotypification = beginGenotypification + beginTableGenotypification + bodyTableGenotypification + endTableGenotypification + endGenotypification;

            var body = '<body>' + genotypification + detail + beginResult + lrResult + correctionResult + endResult + '</body>';

            var report = window.open('', '_blank');
            report.document.write('<html>' + head + body + '</html>');
            report.document.close();
            $(report).on('load', function(){
                report.print();
                report.close();
            });
        };
	}
	
	return ResultsController;
});

