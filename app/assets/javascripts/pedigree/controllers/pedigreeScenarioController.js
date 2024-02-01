define(['angular', 'jquery'], function(angular, $) {
    'use strict';

    var PedigreeScenarioCtrl = function($scope, alertService, pedigreeService, $modal, $timeout, $window,$routeParams,$location) {

        var network;
        // console.log($routeParams);
        // console.log($location);

        $scope.isProcessing = false;
        $scope.previousProfileSearch = "";
        $scope.$parent.elegi(true);

        $scope.drawGraph = function() {
            $scope.createNetwork($scope.scenario.genogram, "genogram_" + $scope.$index, false).then(function(n){
                network = n;
            });
        };

        $scope.removeNode = function(index) {
            $scope.scenario.genogram.splice(index,1);
            $scope.drawGraph();
        };

        $timeout(function(){
            $scope.$watch('scenario.selected', function() {
                if ($scope.scenario.selected) {
                    $scope.drawGraph();
                }
            });
        }, 0);

        $scope.save = function() {
            var promise;
            if ($scope.scenario._id) {
                promise = pedigreeService.updateScenario($scope.scenario);
            } else {
                promise = pedigreeService.createScenario($scope.scenario);
            }

            promise.then(function(response) {
                    alertService.success({message: 'El escenario fue guardado con éxito'});
                    $scope.scenario._id = response.data;
                    $scope.scenario.status = 'Pending';
                },
                function(response) {
                    alertService.error({message: response.data});
                }
            );
        };

        $scope.canValidate = function() {
            if ($scope.scenario.matchId) {
                return true;
            } else {
                return false;
            }
        };

        $scope.delete = function() {
            pedigreeService.changeScenarioStatus($scope.scenario, 'Deleted').then(function() {
                    alertService.success({message: 'El escenario fue eliminado con éxito'});
                    $scope.scenarios.splice($scope.$index,1);
                    $scope.scenario.genogram.forEach(function(node) {
                        if (node.unknown && node.globalCode) {
                            delete $scope.options[node.globalCode];
                        }
                    });
                },
                function(response) {
                    alertService.error({message: response.data});
                }
            );
        };

        $scope.validate = function() {
            var validateScenarioModalInstance = $modal.open({
                templateUrl:'/assets/javascripts/pedigree/views/validate-scenario-modal.html',
                controller: 'validateScenarioModalController',
                resolve: {
                    data: function() {
                        return {
                            scenario: $scope.scenario
                        };
                    }
                }
            });

            validateScenarioModalInstance.result.then(function(operationResult) {
                if (operationResult.status === "Success") {
                    alertService.success({message: 'El escenario fue validado con éxito'});
                    $scope.scenario.status = 'Validated';
                    $scope.validatePedigree();
                } else if (operationResult.status === "Error") {
                    alertService.error({message: operationResult.error});
                }
            });
        };

        $scope.addProfile = function(node) {
            var profilesModalInstance = $modal.open({
                templateUrl:'/assets/javascripts/pedigree/views/search-profile-modal.html',
                controller: 'searchProfileModalController',
                resolve: {
                    data: function () {
                        return {
                            previousSearch: "",
                            idCourtCase: $scope.courtcase.id,
                            noEsEscenario: false,
                            caseType: $scope.courtcase.caseType,
                            profiles: $scope.scenario.genogram
                        };
                    }
                }
            });

            profilesModalInstance.result.then(function (p) {
                node.globalCode = p.globalCode;
                $scope.options[node.globalCode] = node;
                $scope.scenario.lr=undefined;
            });
        };

        $scope.disassociateProfile = function(node) {
            var disassociateModalInstance = $modal.open({
                templateUrl:'/assets/javascripts/pedigree/views/disassociate-profile.html',
                controller: 'disassociateController',
                resolve:
                {
                    node: function() {
                        return node;
                    }
                }
            });

            disassociateModalInstance.result.then(function (value) {
                if (value) {
                    delete $scope.options[node.globalCode];
                    delete node.globalCode;
                    $scope.scenario.lr=undefined;
                }
            });
        };
        
        $scope.getLR = function() {
            $scope.isProcessing = true;
            pedigreeService.getLR($scope.scenario).then(function(response) {
               $scope.scenario.lr = response.data;
                $scope.isProcessing = false;
            }, function() {
                $scope.isProcessing = false;
            });
        };
        
        $scope.disableCalculate = function() {
          var unknowns = $scope.scenario.genogram.filter(function(ind) { return ind.unknown; });
            return !$scope.scenario.frequencyTable || unknowns.length !== 1 || !unknowns[0].globalCode || $scope.processing() || !$scope.scenario._id;
        };

        $scope.printReport = function() {
            network.fit();
            network.selectNodes([]);

            var head = '<head><title>Resultados caso ' + $scope.courtcase.internalSampleCode + '</title>';
            $("link").each(function () {
                head += '<link rel="stylesheet" href="' + $(this)[0].href + '" />';
            });
            head += "</head>";
            // el timeout es necesario para que se termine de cargar la red
            $timeout(function(){
                $scope.canvasURL = $('#genogram_' + $scope.$index + ' canvas').get(0).toDataURL();
                $scope.$apply();
                var report = window.open('', '_blank');
                report.document.write(
                  '<html>' + head +
                  '<body>' + $('#report_'+$scope.$index).html() +
                  '</body></html>'
                );
                report.document.close();
                $(report).on('load', function(){
                    report.print();
                    report.close();
                });
            });
        };
        
        $scope.processing = function() {
          return $scope.isProcessing || $scope.scenario.isProcessing;  
        };
        
        $scope.hasLR = function() {
            return $scope.scenario.lr !== undefined;
        };
        
        $scope.isClosed = function () {
            return $scope.$parent.isClosed();
        };
        
    };

    return PedigreeScenarioCtrl;

});