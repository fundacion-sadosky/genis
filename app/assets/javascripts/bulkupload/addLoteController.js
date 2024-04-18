define(
    [],
    function() {
        'use strict';
        function AddLoteController ($scope, alertService, $filter) {
            $scope.loteInfo = {} ;
            $scope.analysis = ["MT" ,"Autosomal"]
                .map(function (s) {
                    return {label: $filter('i18next')('analysisTypes.' + s)};
                });
            var inicializar = function () {
                $scope.analisis = {};
                $scope.analisis = $scope.analysis[1];
                $scope.tipoAnalisis = "Autosomal";
            };
            inicializar();
            $scope.cancel = function () {
                $scope.$dismiss('cancel');
            };
            $scope.onChange = function(analisis){
                $scope.tipoAnalisis = analisis.label;
            };
            $scope.closeModal = function(files, lote, tipoAnalisis) {
                $scope.loteInfo.files = files;
                $scope.loteInfo.label = lote;
                $scope.loteInfo.analisis = tipoAnalisis;
                $scope.$close($scope.loteInfo);
                console.log(files);
            };
        }
        return AddLoteController;
    });