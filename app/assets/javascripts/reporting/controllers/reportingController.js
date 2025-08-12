define([], function() {
    'use strict';

    function reportingController ($scope, cryptoService, alertService) {

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");

        $scope.search = {};

        $scope.datepickers = {
            hourFrom: false,
            hourUntil: false
        };

        $scope.dateOptions = {
            initDate: new Date()
        };


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
                } else if (fechaDesde){
                    urlPath += "/null"; // or any placeholder you prefer
                }
            }

            var url = cryptoService.encryptBase64(urlPath);
            var a = document.createElement("a");
            document.body.appendChild(a);
            a.style = "display: none";
            a.download = "Reporte Perfiles.pdf";
            a.href = url;
            a.click();
            document.body.removeChild(a);
        };

        $scope.generarReportePorUsuario = function () {
            var urlPath = "/reportes/profilesByUser";  // Base URL
            var url = cryptoService.encryptBase64(urlPath);
            var a = document.createElement("a");
            document.body.appendChild(a);
            a.style = "display: none";
            a.download = "Perfiles Por Usuario.pdf";
            a.href = url;
            a.click();
            document.body.removeChild(a);
        };



        $scope.generarReporteActivosBajaPorCategoria = function () {
            var urlPath = "/reportes/activesInactiveByCategory";  // Base URL
            var url = cryptoService.encryptBase64(urlPath);
            var a = document.createElement("a");
            document.body.appendChild(a);
            a.style = "display: none";
            a.download = "Perfiles Activos y Eliminados por Categoria.pdf";
            a.href = url;
            a.click();
            document.body.removeChild(a);
        };

        $scope.generarReporteEnviados = function () {
            var urlPath = "/reportes/enviadosInstanciaSuperior";  // Base URL
            var url = cryptoService.encryptBase64(urlPath);
            var a = document.createElement("a");
            document.body.appendChild(a);
            a.style = "display: none";
            a.download = "Perfiles Enviados a Instancia Superior.pdf";
            a.href = url;
            a.click();
            document.body.removeChild(a);
        };

        $scope.generarReporteRecibidos = function () {
            var urlPath = "/reportes/recibidosInstanciaInferior";  // Base URL
            var url = cryptoService.encryptBase64(urlPath);
            var a = document.createElement("a");
            document.body.appendChild(a);
            a.style = "display: none";
            a.download = "PerfilesRecibidosInstanciaInferior.pdf";
            a.href = url;
            a.click();
            document.body.removeChild(a);
        };

        $scope.generarReporteCambioCategoria = function () {
            var urlPath = "/reportes/perfilesCambiaronCategoria";  // Base URL
            var url = cryptoService.encryptBase64(urlPath);
            var a = document.createElement("a");
            document.body.appendChild(a);
            a.style = "display: none";
            a.download = "PerfilesCambiaronCategoria.pdf";
            a.href = url;
            a.click();
            document.body.removeChild(a);
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
    }

    return reportingController;
});
