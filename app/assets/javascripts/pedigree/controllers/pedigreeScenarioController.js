define(['angular', 'jquery'], function(angular, $) {
    'use strict';

    var PedigreeScenarioCtrl = function(
      $scope,
      alertService,
      pedigreeService,
      $modal,
      $timeout,
      $window,
      $routeParams,
      $location
    ) {
        var network;

        $scope.isProcessing = false;
        $scope.previousProfileSearch = "";
        $scope.$parent.elegi(true);

        $scope.drawGraph = function() {
            $scope.createNetwork($scope.scenario.genogram, "genogram_" + $scope.$index, false).then(function(n){
                network = n;
                // El tab a veces se activa en el mismo digest en que se
                // instancia la red (al crear un escenario nuevo, o al
                // entrar directo a un escenario desde una notificación)
                // — el contenedor todavia no tiene tamano real (el
                // tab-pane de Bootstrap no termino de mostrarse) y vis.js
                // dibuja el genograma vacio. En vez de esperar un tiempo
                // fijo (que puede no alcanzar si hay mas carga async en
                // paralelo, como al entrar desde una notificación),
                // reintentar hasta que el contenedor tenga tamano real.
                var containerId = "genogram_" + $scope.$index;
                var attempts = 0;
                var maxAttempts = 15;
                var tryFit = function() {
                    attempts++;
                    var hasSize = $('#' + containerId).width() > 0;
                    if (network && (hasSize || attempts >= maxAttempts)) {
                        network.redraw();
                        network.fit();
                    } else if (network) {
                        $timeout(tryFit, 150);
                    }
                };
                $timeout(tryFit, 150);
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
               $scope.scenario.lr = response.data.lr;
               $scope.scenario.markerDetails = response.data.markerDetails;
                $scope.isProcessing = false;
            }, function(response) {
                alertService.error({message: response.data});
                $scope.isProcessing = false;
            });
        };
        
        $scope.disableCalculate = function() {
          var unknowns = $scope.scenario.genogram.filter(function(ind) { return ind.unknown; });
            return !$scope.scenario.frequencyTable || unknowns.length !== 1 || !unknowns[0].globalCode || $scope.processing() || !$scope.scenario._id;
        };

        // pedigreeScenarioReportController (controller del div #report_N)
        // carga la comparacion de perfiles de forma asincronica y avisa
        // via $emit cuando termina. Si "Imprimir" se clickea antes de eso,
        // el reporte clonado sale con las filas de la tabla pero sin los
        // alelos (todavia no cargaron). reportReadyIndexes trackea que
        // escenarios ya avisaron que terminaron de cargar.
        var reportReadyIndexes = {};
        $scope.$on('scenarioReportReady', function(event, index) {
            reportReadyIndexes[index] = true;
        });

        $scope.printReport = function() {
            // network.fit(); // Center and adjust network size to canvas size.
            if (network) {
                network.selectNodes([]);
            }

            var head = '<head><title>Resultados caso ' + $scope.courtcase.internalSampleCode + '</title>';
            $("link").each(function () {
                head += '<link rel="stylesheet" href="' + $(this)[0].href + '" />';
            });
            head += "</head>";
            var doPrint = function(){
                // el timeout es necesario para que se termine de cargar la red
                $timeout(function(){
                    $scope.canvasURL = $('#genogram_' + $scope.$index + ' canvas').get(0).toDataURL();
                    $scope.$apply();
                    var report = window.open('', '_blank');
                    if (!report) {
                        // window.open puede devolver null si el navegador
                        // bloquea el popup (comun cuando se abre fuera de
                        // la ventana de "user gesture" del click, ej. tras
                        // esperar varios segundos la carga de datos).
                        alertService.error({message: 'El navegador bloqueó la ventana de impresión. Habilite los popups para este sitio e intente de nuevo.'});
                        return;
                    }
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

            if (reportReadyIndexes[$scope.$index]) {
                doPrint();
            } else {
                // Todavia no cargo la comparacion de perfiles: esperar el
                // aviso (o como maximo 5 segundos, para no dejar el click
                // en "Imprimir" colgado si la carga falla) antes de
                // imprimir, asi el reporte clonado incluye los datos.
                var printed = false;
                var unwatch = $scope.$on('scenarioReportReady', function(event, index) {
                    if (!printed && index === $scope.$index) {
                        printed = true;
                        unwatch();
                        doPrint();
                    }
                });
                $timeout(function() {
                    if (!printed) {
                        printed = true;
                        unwatch();
                        doPrint();
                    }
                }, 5000);
            }
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