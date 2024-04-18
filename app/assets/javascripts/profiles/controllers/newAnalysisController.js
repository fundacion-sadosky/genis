define(['angular'], function(angular) {
    'use strict';

    function NewAnalysisController(
        $sce, $scope , $routeParams, $log, profileService, Upload, resourcesHelper,
        $location, $modal, cryptoService, alertService , appConf) {

        $scope.uploadedAnalysis = null;
        $scope.selectedAlleles = [];
        $scope.newAnalysis = {};
        $scope.chkAnalysis = {};
        $scope.loci = [];
        $scope.lab = "-"+appConf.labCode+"-";

        $scope.saveUploaded = function() {
            profileService.saveUploaded($scope.uploadedAnalysis.token).then(
                function(response) {
                    $scope.ejecucionOk = true;
                    if ($routeParams.profileId){
                        $location.url("/profile/" + $routeParams.profileId + '?f=true');
                    } else{
                        $location.path("/profile/" + response);
                    }
                },
                function(response) {
                    $log.log(response);
                    alertService.error({message: 'Han ocurrido errores. ' + response.data.join('. ')});
                });
        };
        
        var filesId = {};
        var rawFilesId = {};

        $scope.status = {current:'fill'};
        var electropherogramHolderImage = 'assets/images/noelectropherogram.jpg';
        $scope.isProcessing = false;

        $scope.trustAsHtml = $sce.trustAsHtml;

        $scope.contributors = [1,2,3,4];

        $scope.findLoci = function(idKit) {
            if (!idKit) { return; }

            $scope.disableLabels();

            profileService.getLociByStrKitId(idKit).then(
                function(response) {
                    if($scope.kitsById[idKit].type===4){
                        $scope.loci = [];
                    }else{
                        $scope.loci = response.data;
                    }

                    $scope.newAnalysis = {
                        globalCode : $scope.profile._id,
                        token: filesId,
                        genotypification : {},
                        kit : idKit,
                        type: $scope.kitsById[idKit].type,
                        tokenRawFile: rawFilesId,
                        nameRawFile: ''
                        //contributors: $scope.profile.contributors
                    };

                    if ($scope.profile.labelable) { $scope.profile.labeledGenotypification = {}; } // TODO: Ver en donde poner la labeledGenotypification para que pueda ser utilizada desde dna.html y desde pdg-locus.html

                    $scope.chkAnalysis = {
                        index: 0,
                        genotypification: {}
                    };

                    $scope.loci.forEach(function(locus) {
                        $scope.formatLocus($scope.newAnalysis, $scope.chkAnalysis, locus);
                    });
                },
                function() {
                    alertService.error({message: 'Ha ocurrido un error'});
                });
        };

        function addCarouselItem (analysisId, newCarouselItem) {
            var analysis = $scope.profile.analyses.filter(function(x){return x.id === analysisId;})[0];
            analysis.efgsCarousel.push(newCarouselItem);
        }

        //////////////////////////////////////
        $scope.inprints = [electropherogramHolderImage];

        function sucessImagesPost(analysisId){
            return function(data){
                var newCarrouselItem = "/resources/temporary/" + data.substring(5);

                if (analysisId) {
                    addCarouselItem(analysisId, newCarrouselItem);
                } else {
                    $scope.inprints.push(newCarrouselItem);
                    if($scope.inprints[0] === electropherogramHolderImage) {
                        $scope.inprints.shift();
                    }
                }
            };
        }

        var errorImagePost=function(){
            $log.log('error al mandar las imagenes');
        };

        resourcesHelper.getFilesId().then(
            function(response){
                filesId = response.data.filesId;
            });
        resourcesHelper.getFilesId().then(
            function(response){
                rawFilesId = response.data.filesId;
            });
        $scope.addElectropherograms = function(idAnalysis,name){
            profileService.addElectropherograms(filesId, $scope.currentGlobalCode, idAnalysis,name).then(function(response) {
                if (response.data) {
                    alertService.error({message: 'Error guardando las imagenes ' + response.data});
                } else {
                    alertService.success({message: 'Se insertaron las nuevas imagenes'});
                }
            });
        };

        $scope.currentAnalisysId = null;
        $scope.onAddEpgToExistingAnalysis = function($files, analysisId) {
            $scope.onFileSelect($files, analysisId);
        };
        $scope.onAddFileToExistingAnalysis = function($files, analysisId) {
            $scope.onRawFileSelect($files, analysisId);
        };
        $scope.onRawFileSelect = function($files, analysisId) {
            if ($files && $files.length !== 0) {
                $scope.currentAnalisysId = analysisId;
                var url = cryptoService.encryptBase64('/uploadFile');
                for (var i = 0; i < $files.length; i++) {
                    var file = $files[i];
                    $scope.newAnalysis.nameRawFile = file.name;
                    $scope.upload = Upload.upload({
                        url: url,
                        method: 'POST',
                        fields: {
                            filesId: rawFilesId
                        },
                        file: file
                    });
                }
            }
        };
        $scope.onFileSelect = function($files, analysisId) {
            if($files && $files.length !== 0){
                $scope.currentAnalisysId = analysisId;
                var url = cryptoService.encryptBase64('/uploadImage');
                for (var i = 0; i < $files.length; i++) {
                    var file = $files[i];
                    $scope.upload = Upload.upload({
                        url: url,
                        method: 'POST',
                        fields: {filesId: filesId},
                        file: file
                    })
                        .success( sucessImagesPost(analysisId) )
                        .error( errorImagePost);
                }
            }
        };

        $scope.setStrigency = function() {
            var strigencyModalInstance = $modal.open({
                templateUrl:'/assets/javascripts/profiles/views/stringency-modal.html',
                controller: 'stringencyModalController',
                resolve: {
                    data: function () {
                        return {
                            subcategory: $scope.subcategory,
                            profile: $scope.profile,
                            subcatsRel: $scope.subcatsRel,
                            mismatches: $scope.mismatches
                        };
                    }
                }
            });

            strigencyModalInstance.result.then(function (ret) {
                $scope.subcatsRel = ret.subcatsRel;
                $scope.mismatches = ret.mismatches;
            }, function () {
                //$log.log('Modal dismissed at: ' + new Date());
            });
        };

        $scope.getAnalysisName = function (item){
            if (item && $scope.analysisTypes) {
                return $scope.analysisTypes[item.type].name;
            }
        };

        $scope.getPossibleLocus = function() {
            return $scope.locusOptions;
        };
        $scope.addLocus = function(newLocus) {

            if (!$scope.newAnalysis || !$scope.newAnalysis.genotypification) {
                $scope.newAnalysis = {
                    globalCode : $scope.profile._id,
                    token: filesId,
                    genotypification : {},
                    type: $scope.activeAnalysis
                };
            }
            if (!$scope.chkAnalysis || !$scope.chkAnalysis.genotypification) {
                $scope.chkAnalysis = {
                    index: 0,
                    genotypification: {}
                };
            }

            if (newLocus) {
                $scope.disableLabels();
                var locusGroup = angular.copy($scope.locusById[$scope.locusById[newLocus].idGroup]);
                var locus = $scope.locusById[newLocus];
                $scope.initLocus($scope.newAnalysis, $scope.chkAnalysis,locusGroup);

                $scope.formatLocusDna($scope.newAnalysis, $scope.chkAnalysis, locus);

                $scope.loci.push(locusGroup);
                // $scope.addedLocus.push(locusGroup);

            }
        };
        $scope.locusAlreadyAdded = function(item) {
            return $scope.loci.filter(function(l) { return l.id === item.id || (angular.isDefined(l.idGroup) && angular.isDefined(item.idGroup) && l.idGroup === item.idGroup) ; }).length === 0;
        };
        $scope.removeLocus = function(index) {
            var locusId =  $scope.loci[index].id;
            if(locusId){
                delete $scope.newAnalysis.genotypification[locusId];
                delete $scope.chkAnalysis.genotypification[locusId];
            }
            $scope.loci.splice(index, 1);
        };
        
    }

    return NewAnalysisController;

});