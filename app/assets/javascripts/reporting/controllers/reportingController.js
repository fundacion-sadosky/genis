define([], function() {
    'use strict';

    function reportingController ($scope, $http, cryptoService, alertService) {

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");

        $scope.search = {};
        $scope.procesando = false;

        $scope.datepickers = {
            hourFrom: false,
            hourUntil: false
        };

        $scope.dateOptions = {
            initDate: new Date()
        };

        function downloadFile(urlPath, filename) {
            $scope.procesando = true;
            $http.get(urlPath, {responseType: 'blob'}).then(
                function(response) {
                    var blob = new Blob([response.data], {type: response.headers('content-type')});
                    var blobUrl = URL.createObjectURL(blob);
                    var a = document.createElement("a");
                    document.body.appendChild(a);
                    a.style = "display: none";
                    a.download = filename;
                    a.href = blobUrl;
                    a.click();
                    setTimeout(function() {
                        URL.revokeObjectURL(blobUrl);
                        document.body.removeChild(a);
                    }, 100);
                    $scope.procesando = false;
                },
                function() {
                    alertService.error({message: 'Error al generar el reporte.'});
                    $scope.procesando = false;
                }
            );
        }

        $scope.generarReporte = function () {
            var fechaDesde = $scope.search.hourFrom ? formatDate($scope.search.hourFrom) : null;
            var fechaHasta = $scope.search.hourUntil ? formatDate($scope.search.hourUntil) : null;

            var urlPath = "/reportes/profilesReporting";  // Base URL

            if (fechaDesde || fechaHasta) {  // Only add dates if at least one is present
                urlPath += "/"; // Add a slash to separate the base URL

                if (fechaDesde) {
                    urlPath += fechaDesde;
                }

                if (fechaHasta) {
                    urlPath += "/" + fechaHasta;  // Add the second date, include the slash
                } else if (fechaDesde) {
                    urlPath += "/null"; // or any placeholder you prefer
                }
            }

            downloadFile(urlPath, "Reporte Perfiles.pdf");
        };

        $scope.generarReportePorUsuario = function () {
            downloadFile("/reportes/profilesByUser", "Perfiles Por Usuario.pdf");
        };

        $scope.generarReporteActivosBajaPorCategoria = function () {
            downloadFile("/reportes/activesInactiveByCategory", "Perfiles Activos y Eliminados por Categoria.pdf");
        };

        $scope.generarReporteEnviados = function () {
            downloadFile("/reportes/enviadosInstanciaSuperior", "Perfiles Enviados a Instancia Superior.pdf");
        };

        $scope.generarReporteRecibidos = function () {
            downloadFile("/reportes/recibidosInstanciaInferior", "PerfilesRecibidosInstanciaInferior.pdf");
        };

        $scope.generarReporteCambioCategoria = function () {
            downloadFile("/reportes/perfilesCambiaronCategoria", "PerfilesCambiaronCategoria.csv");
        };

        function formatDate(date) {
            return date.getDate() + "-" + date.getMonth() + "-" + date.getFullYear();
        }

        $scope.checkMax = function (fieldName) {
            if (!$scope.search[fieldName]) return; // Exit if the field is empty

            var aux = $scope.search[fieldName];
            var today = new Date();

            if (today - aux < 0) {
                alertService.info({message: 'La fecha debe ser anterior a la actual.'});
                $scope.search[fieldName] = undefined;
                $scope.minDateCoin = null;
            } else {
                if (fieldName === 'hourFrom') {
                    $scope.minDateCoin = aux;
                }
            }

        };

        $scope.toggleDatePicker = function ($event, witch) {
            $event.preventDefault();
            $event.stopPropagation();

            $scope.datepickers[witch] = !$scope.datepickers[witch];
        };

        $scope.checkMaxMin = function (fieldName, fechaMin) {
            if (!$scope.search[fieldName]) return; // Exit if the field is empty

            var aux = $scope.search[fieldName];
            var min = $scope.search[fechaMin];
            var max = new Date();

            if (min === undefined || min === null) {
                min = $scope.search.hourFrom;
            }

            if (max - aux < 0) {
                alertService.info({message: 'La fecha debe ser anterior a la actual.'});
                $scope.search[fieldName] = undefined;
            } else {
                if (min && min - aux > 0) { // Only compare if 'min' has a value
                    alertService.info({message: 'La fecha debe ser posterior al campo desde.'});
                    $scope.search[fieldName] = undefined;
                }
            }
        };

        $scope.generarListadoComleto = function () {
            downloadFile("/reportes/listadoCompleto", "TodosLosPerfiles.csv");
        };

        $scope.generarListedoCoincidencias = function () {
            downloadFile("/reportes/listadoCoincidencias", "PerfilesCoincidentes.csv");
        };

        $scope.generarListedoReplicadosAInstanciaSuperior = function () {
            downloadFile("/reportes/generarListedoReplicadosAInstanciaSuperior", "PerfilesReplicadosAInstanciaSuperior.csv");
        };

        $scope.generarListedoRecibidosInstanciasInferiores = function () {
            downloadFile("/reportes/generarListedoRecibidosInstanciasInferiores", "PerfilesRecibidosDeInstanciasInferiores.csv");
        };


    }

        return reportingController;
});
