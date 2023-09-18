define(['angular', 'jquery','lodash'], function(angular, $,_) {
'use strict';

function ProfileController(
	$scope , $rootScope, $routeParams, $log, profileService, analysisTypeService,
	$route, $location, profileHelper, $modal, cryptoService, alertService, locusService, kitService,Upload,resourcesHelper, appConf) {
    $scope.regions = [];
    $scope.addedRegions = [];
    $scope._RANGE = '_RANGE';
    $scope.lab = "-"+appConf.labCode+"-";
    $scope._ = _;
    $scope.mt = {"HV1":{},"HV2":{},"HV3":{},"HV4":{},"HV1_RANGE":{},"HV2_RANGE":{},"HV3_RANGE":{},"HV4_RANGE":{}};

    localStorage.removeItem("searchPedigree");
    localStorage.removeItem("searchMatches");
    localStorage.removeItem("searchPedigreeMatches");
    localStorage.removeItem("nuevo");

	locusService.list().then(function(response) {
        $scope.locusById = {};
        var data = [];
        response.data.forEach(function(l) {
            if(l.id.endsWith("_RANGE")){
                var lCopy = _.cloneDeep(l);
                lCopy.id = lCopy.id.replace("_RANGE","_2_RANGE");
                lCopy.name = lCopy.name + " 2 ";
                data.push(lCopy);
            }
            data.push(l);
        });
        data.forEach(function(l) {
            $scope.locusById[l.id] = l;
        });

        $scope.locusOptions = data.map(function(l){ l.idGroup = angular.copy(l.id) ;l.idGroup = l.idGroup .replace("_2_RANGE","_RANGE"); return l;});


    });
    $scope.isOutOfLadder = locusService.isOutOfLadder;

    kitService.list().then(function(response) {
        $scope.kitsById = {};
        response.data.forEach(function(k) {
            if(k.type!==4){
                $scope.kitsById[k.id] = k;
            }
        });
    });

    analysisTypeService.listById().then(function(response) {
        $scope.analysisTypes = response;
        if (!$scope.activeAnalysis) {
            $scope.activeAnalysis = parseInt(Object.keys($scope.analysisTypes)[0]);
        }
    });

	$scope.$watch('activeAnalysis', function() {
        // when the selection changes, select the first tab (dna) by default
        if ($scope.activeAnalysis && $scope.activeAnalysis !== -1) {
            $scope.analysisTypes[$scope.activeAnalysis].active = true;
        }
    });

    var showDnaTab = function(show) {
        $scope.showAdd = !show;
        $scope.showDna = show;
    };

    var resolveQs = function() {
        var qs = $location.search();
        if(qs.tab){
            if(qs.tab === 'add'){
                showDnaTab(false);
                $scope.activeAnalysis = -1;
            }else{
                showDnaTab(true);
            }
        } else {
            showDnaTab(true);
        }
    };

    resolveQs();

	var modalInstance = null;
	
	var cleanGlobalVar = function() {
		$scope.profile = {};
		$scope.category = {};
		$scope.subcategory = {};
		$scope.subcatsRel = null;
		$scope.currentGlobalCode = {};
	};

	cleanGlobalVar();
    
    $scope.enableLabelsStatus = false;
    $scope.enableLabels = function() {
        $scope.enableLabelsStatus = true;
    };
	$scope.disableLabels = function() {
		$scope.enableLabelsStatus = false;
	};
    $scope.getStoredRegions = function(genotipification) {
        return Object.keys(genotipification).sort().filter(function(x){return !x.endsWith("_RANGE");});
    };
	var findProfile = function(globalCode) {
		cleanGlobalVar();
		
		profileService.getProfile(globalCode).then(
			function(response) {

                $scope.labelSets = response.data.labelsSets;
				$scope.labels = response.data.labels;
				$scope.profile._id = response.data.profileData.globalCode;
				$scope.isDeleted = response.data.profileData.deleted;
				$scope.profile.globalCode =  response.data.profileData.globalCode;	
				$scope.profile.internalSampleCode =  response.data.profileData.internalSampleCode;
				
				$scope.category = response.data.group;
				$scope.subcategory = response.data.category;
				$scope.profile.labelable = response.data.labelable;
				$scope.profile.editable = response.data.editable;
                $scope.profile.readOnly = response.data.readOnly;
                $scope.profile.isUploadedToSuperior = response.data.isUploadedToSuperior;
                $scope.profile.associable = response.data.associable;
                $scope.profile.isReference = response.data.isReference;

				$scope.isProcessing = false;
				
				$scope.profile.genotypification = {};
				
				function sortAnalysisGenotypification(analysis) {
					profileService.getLociByStrKitId(analysis.kit).then(function(response){
						if (response.data.length > 0) {
                            var sortedGenotypification = {};
                            for (var i = 0; i < response.data.length; i++) {
                                var locus = response.data[i].id;
                                if (analysis.genotypification[locus]) {
                                    sortedGenotypification[locus] = analysis.genotypification[locus];
                                }
                            }
                            analysis.genotypification = sortedGenotypification;
                        }
					});
				}
				
				if (response.data.globalCode){
					$scope.profile.genotypification = response.data.genotypification;
					if(!_.isUndefined($scope.profile.genotypification[4])){
                        $scope.storedRegions = $scope.getStoredRegions($scope.profile.genotypification[4]);
                        $scope.possibleRegions = _.difference(['HV4','HV3','HV2','HV1'], Object.keys($scope.profile.genotypification[4]));
                    }else{
                        $scope.possibleRegions =['HV4','HV3','HV2','HV1'];
                    }
                    $scope.profile.analyses = response.data.analyses;
					
					$scope.electropherograms = [];
                    $scope.files = [];
                    $scope.fileUploadedToModel = function(f){
                        return {path:cryptoService.encryptBase64('/profiles/' + $scope.currentGlobalCode + '/file/' + f.fileId),name:f.name, fileId: f.fileId};
                    };
                    $scope.epgUploadedToModel = function(f){
                        return {path:cryptoService.encryptBase64('/profiles/' + $scope.currentGlobalCode + '/epg/' + f.fileId),name:f.name, fileId: f.fileId};
                    };
                    $scope.profile.analyses.forEach(function(analysis) {
						$scope.electropherograms = $scope.electropherograms.concat(analysis.efgs);
                        $scope.files = $scope.files.concat(analysis.files);

                        analysis.efgsCarousel = analysis.efgs.map(function(efgId){return cryptoService.encryptBase64('/profiles/' + $scope.currentGlobalCode + '/epg/' + efgId.fileId);});
                        analysis.fileList = analysis.files.map($scope.fileUploadedToModel);
                        analysis.electropherogramsList = analysis.efgs.map($scope.epgUploadedToModel);

                        sortAnalysisGenotypification(analysis);
					});

					$scope.electropherograms = $scope.electropherograms.map(function(e){
						return cryptoService.encryptBase64("/profiles/" + $scope.currentGlobalCode + "/epg/" + e.fileId);
                    });
                    $scope.files = $scope.files.map($scope.fileUploadedToModel);
					if (response.data.contributors) {
                        $scope.profile.contributors = response.data.contributors;
                    } else if ($scope.profile.isReference) {
                        $scope.profile.contributors = 1;
                    }

					$scope.enableLabelsStatus = !$scope.profile.isReference && $scope.profile.contributors > 1;
					$scope.isMixture = $scope.enableLabelsStatus;
					
					$scope.profile.labeledGenotypification = angular.extend({}, response.data.labeledGenotypification);
					
					if (response.data.matchingRules) { 
						$scope.subcatsRel = response.data.matchingRules; }
					
					if (response.data.mismatches) { 
						$scope.mismatches = response.data.mismatches; }

					getAssociated();
				}else{
                    $scope.possibleRegions =['HV4','HV3','HV2','HV1'];
                }

			},
			function(response) {
				if (response.status === 400 || response.status === 404) {
					alertService.warning({message: 'No se ha encontrado un perfil para ' + globalCode});
				}
				else {
					alertService.error({message: 'Ha ocurrido un error'});
				}
				$scope.isProcessing = false;
			}
		);
			

		var getAssociated = function() {
            var labels = Object.keys($scope.labels);
            if (labels.length > 0 && isNaN(labels[0])) {
                profileService.getProfile(labels[0]).then(function(response) {
                    $scope.associated = response.data;
                });
            }
		};
		
		$scope.currentGlobalCode = globalCode;
	};

    $scope.initLocus = function(newAnalysis, checkAnalysis, locus) {
        locus.maximumAllelesQty = $scope.profile.isReference? locus.maximumAllelesQty : 30;
        locus.alleleQty = locus.minimumAllelesQty;
        locus.errorMsg = "";
    };
    $scope.formatLocus = function(newAnalysis, checkAnalysis, locus) {
        locus.maximumAllelesQty = $scope.profile.isReference? locus.maximumAllelesQty : 30;
        locus.alleleQty = locus.minimumAllelesQty;
        newAnalysis.genotypification[locus.id] = [];
        checkAnalysis.genotypification[locus.id] = [];
        locus.errorMsg = "";

        if ($scope.default.hasOwnProperty(locus.id)) {
            newAnalysis.genotypification[locus.id] = $scope.default[locus.id];
        }
    };
    $scope.formatLocusDna = function(newAnalysis, checkAnalysis, locus) {

        newAnalysis.genotypification[locus.idGroup] = [];
        checkAnalysis.genotypification[locus.idGroup] = [];

        console.log('locus 193',locus);
        if ($scope.default.hasOwnProperty(locus.id)) {
            newAnalysis.genotypification[locus.idGroup] = $scope.default[locus.id];
        }

    };

	findProfile($routeParams.profileId);
	
	$scope.isDblValidationNeeded = function(newAnalysis, checkAnalysis, loci){
		var locusObj = loci[checkAnalysis.index];
		
		// Check for empty loci
		var len = (locusObj) ? profileHelper.getNonWcAlleles(newAnalysis.genotypification[locusObj.id]).length : 0;
        var analysis = ($scope.activeAnalysis > -1)? $scope.activeAnalysis : $scope.kitsById[newAnalysis.kit].type;

        if ($scope.analysisTypes[analysis].mitochondrial) {
            return true;
        } else {
            return !(len > 0 &&
            $scope.profile.genotypification &&
            $scope.profile.genotypification[analysis][locusObj.id] &&
            profileHelper.getNonWcAlleles($scope.profile.genotypification[analysis][locusObj.id]).length === len);
        }
	};
			
	$scope.closeNewAnalysisConfirmationModal = function(save, analysis){
        modalInstance.close();
		if (save) {
			$scope.save(analysis);
		}
	};

	$scope.isEmpty = function(analysis) {
		var geno = analysis.genotypification;
		for ( var key in geno) {
			var alleles = geno[key]; 
			for (var i = 0; i < alleles.length; i++) {
				if(alleles[i].trim().length > 0) {
					return false;
				} 
			}
		}
		return true;
	};
	
	$scope.verifyNewAnalysis = function(newAnalysis, checkAnalysis, loci, status) {
		var isDblCheckNeeded = true;
		profileHelper.clearEmptyLoci(newAnalysis.genotypification); // Check for empty alleles in the entire genotypification array
		
		profileHelper.verifyDuplicates(newAnalysis.genotypification, loci, $scope.profile.isReference);
        var analysis = ($scope.activeAnalysis > -1)? $scope.activeAnalysis : $scope.kitsById[newAnalysis.kit].type;

		// If there's an existing profile => Verify against it
		if (Object.keys($scope.profile.genotypification).length > 0) {
            var activeGenotypification = $scope.profile.genotypification.hasOwnProperty(analysis)?
                $scope.profile.genotypification[analysis]:{};
            if ($scope.analysisTypes[analysis].mitochondrial) {
                if(!profileHelper.verifyMitochondrial(newAnalysis.genotypification, $scope.mt)){
                    return;
                }
            } else {
                isDblCheckNeeded = profileHelper.verifyGenotypification(
                    activeGenotypification,
                    newAnalysis.genotypification,
                    loci);
            }
		} else if ($scope.analysisTypes[analysis].mitochondrial) {
            if(!profileHelper.verifyMitochondrial(newAnalysis.genotypification, $scope.mt)){
                return;
            }
		}

		if (loci.filter(function(x){return x.errorMsg.length > 0;}).length > 0) { return; } // if there are errors => return
		
		if (!isDblCheckNeeded){
			modalInstance = $modal.open({
				templateUrl:'/assets/javascripts/profiles/views/new-analysis-confirmation-modal.html',
				controller: 'newAnalysisConfirmationController',
                resolve: {
                    newAnalysis: function () {
                        return newAnalysis;
                    },
                    controller: function () {
                        return $scope;
                    }
                }
			}); 
			return;
		}
		
		// Continue with the dblCheck 
		status.current = 'dblCheck';
        // status.statusMtRange = 'dblCheck';
        // status.statusMtVars = 'dblCheck';
		var len = 0;
		var dblValidation = true;
		do {
			var locusObj = loci[checkAnalysis.index];
			
			// Check for empty loci
			len = (locusObj) ? newAnalysis.genotypification[locusObj.id].length : 0;

			// Check whether a double validation is needed
			dblValidation = $scope.isDblValidationNeeded(newAnalysis, checkAnalysis, loci);
				
			if (len === 0 || !dblValidation) { checkAnalysis.index++; }
		}
		while ((len === 0 || !dblValidation) && checkAnalysis.index < loci.length);
	};
	
	var cleanArray = function(vector, deleteValue) {
		var array = angular.copy(vector);
		for (var i = 0; i < array.length; i++) {
			if (array[i] === deleteValue) {         
				array.splice(i, 1);
				i--;
			}
		}
		return array;
	};
	
	$scope.validateLocus = function(newAnalysis, checkAnalysis, locus, loci) {
		var srcOrg = newAnalysis.genotypification[locus];
		var src = cleanArray(srcOrg,"");
		var check = checkAnalysis.genotypification[locus];
		var isValid = (src+'') === (check+'');
		
		var len = 0;
		var continueWithoutDblValidation = false;
		if(isValid)	{
			do {
                checkAnalysis.index++;
				var locusObj = loci[checkAnalysis.index];

				// Check for empty loci
				len = (locusObj)? newAnalysis.genotypification[locusObj.id].length: 0;
				
				// Check whether a double validation is needed
				continueWithoutDblValidation = !$scope.isDblValidationNeeded(newAnalysis, checkAnalysis, loci);
			} while ((len === 0 || continueWithoutDblValidation ) && checkAnalysis.index < loci.length);
		}
	};
    $scope.validateLocusMt = function(newAnalysis, checkAnalysis, locus, loci,index) {
      console.log("In validateLocusMt");
        var srcOrg = newAnalysis.genotypification[locus];
        var src = cleanArray(srcOrg,"");
        var check = checkAnalysis.genotypification[locus];
        var isValid = (src+'') === (check+'');

        var len = 0;
        var continueWithoutDblValidation = false;
        if(isValid)	{
            do {
                checkAnalysis[index]++;
                var locusObj = loci[checkAnalysis[index]];

                // Check for empty loci
                len = (locusObj)? newAnalysis.genotypification[locusObj.id].length: 0;

                // Check whether a double validation is needed
                continueWithoutDblValidation = !$scope.isDblValidationNeeded(newAnalysis, checkAnalysis, loci);
            } while ((len === 0 || continueWithoutDblValidation ) && checkAnalysis[index] < loci.length);
        }
    };
	$scope.save = function(newAnalysis) {
		
		var k = $scope.kitsById[newAnalysis.kit];
		if (k && $scope.analysisTypes[k.type].mitochondrial || $scope.analysisTypes[newAnalysis.type].mitochondrial) {
            newAnalysis.genotypification = 
				profileHelper.getMtGenotyfication(newAnalysis.genotypification);
		}
		
		if ($scope.isMixture && $scope.labelsAreUsed()) { newAnalysis.labeledGenotypification = $scope.profile.labeledGenotypification; }
		if ($scope.subcatsRel) { newAnalysis.matchingRules = $scope.subcatsRel; }
		if ($scope.mismatches) { newAnalysis.mismatches = $scope.mismatches; }

		profileService.saveFirstAutosomal(newAnalysis).then(
			function(response) {
				if (response.data) {
					$log.log("profileService.saveFirstAutosomal returns: ");
					$log.log(response.data);
					if(response.data.validationErrors){
						alertService.error({message: response.data.validationErrors.join('. ')});
					}else{
						alertService.success({message: 'El análisis se guardo satisfactoriamente. Proceso de match en progreso...'});
                        if ($routeParams.profileId){
							$rootScope.f = true;
							$rootScope.m = (response.data.matcheable === undefined) ? true: response.data.matcheable;
							$route.reload();
						}
						else{
							$location.path("/profile/" + newAnalysis.globalCode);
						}
					}
				}
			}, 
			function(response) {
				alertService.error({message: response.data.join('. ')});
			});
	};

	$scope.doCancel = function(){
		$route.reload();
	};
	$scope.labelsAreUsed = function() {
		return angular.isObject($scope.profile.labeledGenotypification) &&
			Object.keys($scope.profile.labeledGenotypification).some(
				function(l){return Object.keys($scope.profile.labeledGenotypification[l]).length > 0;});
	};

    $scope.fadeIn = function(id) {
        $("#" + id).hide();
        $("#" + id).fadeIn();
    };

    $scope.selectAnalysis = function(id) {
        if (id !== -1) {
            $scope.fadeIn("profile-tabs");
        }

        if ($scope.activeAnalysis === -1) {
            showDnaTab(true);
            $scope.activeAnalysis = id;
        }

        if ($scope.activeAnalysis !== id) {
            $scope.activeAnalysis = id;
        }
    };

    $scope.goToNewAnalysis = function() {
        $scope.fadeIn("new-analysis");
        showDnaTab(false);
        $scope.activeAnalysis = -1;
    };

    $scope.getKits = function() {
        if ($scope.kitsById) {
            return Object.keys($scope.kitsById).map(function (a) {
                return $scope.kitsById[a];
            });
        }
    };

    $scope.getKitName = function(kit) {
        if(!angular.isDefined($scope.kitsById)){
            return "";
        }
        if ($scope.kitsById.hasOwnProperty(kit)) {
            return $scope.kitsById[kit].name;
        } else {
            return kit;
        }
    };

    $scope.uploadProfile = function() {

        profileService.uploadProfile($scope.profile._id).then(function() {
            alertService.success({
                message: 'Se subió el perfil'
            });
        }, function(response) {
            alertService.error({
                message: 'Error: ' + response.data.message
            });
        });
    };

	$scope.default = profileHelper.defaultMarkers();

    // resourcesHelper.getFilesId().then(
    //     function(response){
    //         filesId = response.data.filesId;
    //         $log.log('profileController:*+*+*+*+*+*+ ' + filesId);
    //     });

    $scope.onAddEpgToExistingAnalysis = function($files, analysisId) {
        $scope.onFileSelect($files, analysisId);
    };
    $scope.onAddFileToExistingAnalysis = function($files, analysisId) {
        $scope.onRawFileSelect($files, analysisId);
    };
    $scope.title = "Confirma eliminar archivo";
    $scope.content = "¿Realmente desea eliminar el archivo seleccionado?";
    $scope.close = function(confirm) {
        console.log("close confirm file");

        var file = $scope.fileSelected;
        var analysis = $scope.analysisSelected;

        $scope.confirmRemoveFile.close();
        if(confirm){
            profileService.removeFile(file.fileId).then(function(response) {
                alertService.success({message: 'Se eliminó el archivo'});
                console.log("removeFile ok",response);
                if(analysis && analysis.fileList && analysis.fileList.length){
                    analysis.fileList.splice(analysis.fileList.indexOf(file), 1);
                }
            },function(response) {
                console.log("removeFile no ok");
                alertService.error({message: response.data.error});
            });
        }
        $scope.fileSelected = undefined;
        $scope.analysisSelected = undefined;
    };
    $scope.closeRemoveEpg = function(confirm) {
        console.log("closeRemoveEpg");

        var file = $scope.fileSelected;
        var analysis = $scope.analysisSelected;

        $scope.confirmRemoveFile.close();
        if(confirm){
            profileService.removeEpg(file.fileId).then(function(response) {
                console.log("removeEpg ok",response);
                alertService.success({message: 'Se eliminó el electroferograma'});

                if(analysis && analysis.electropherogramsList && analysis.electropherogramsList.length){
                    var indexFile = analysis.electropherogramsList.indexOf(file);
                    analysis.electropherogramsList.splice(indexFile, 1);
                    analysis.efgsCarousel.splice(indexFile, 1);
                }
            },function(response) {
                console.log("removeEpg no ok");
                alertService.error({message: response.data.error});
            });
        }
        $scope.fileSelected = undefined;
        $scope.analysisSelected = undefined;
    };
    $scope.removeFile = function(file,analysis) {
        $scope.fileSelected = file;
        $scope.analysisSelected = analysis;
        $scope.confirmRemoveFile = $modal.open({
            templateUrl: '/assets/javascripts/common/directives/pdg-confirm.html',
            scope: $scope
        });

    };
    $scope.removeEpg = function(file,analysis) {
        $scope.fileSelected = file;
        $scope.analysisSelected = analysis;
        $scope.confirmRemoveFile = $modal.open({
            templateUrl: '/assets/javascripts/common/directives/pdg-confirm-remove-file.html',
            scope: $scope
        });

    };
    $scope.onFileSelect = function($files, analysisId) {
        resourcesHelper.getFilesId().then(
            function(response){
                var filesId = response.data.filesId;

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
                    .success( sucessImagesPost(analysisId,filesId,file.name) )
                    .error( errorImagePost);
            }
        }
            });
    };
    $scope.filterFiles = function(path,item){return item.path === path;};
    $scope.filterEmgById = function(fileId,item){return item.fileId === fileId;};

    $scope.onRawFileSelect = function($files, analysisId) {
        resourcesHelper.getFilesId().then(
            function(response){
                var filesId = response.data.filesId;
                if($files && $files.length !== 0){
                    $scope.currentAnalisysId = analysisId;
                    var url = cryptoService.encryptBase64('/uploadFile');
                    for (var i = 0; i < $files.length; i++) {
                        var file = $files[i];
                        $scope.upload = Upload.upload({
                            url: url,
                            method: 'POST',
                            fields: {filesId: filesId},
                            file: file
                        });
                        $scope.upload .success( sucessFilePost(analysisId,filesId,file.name) );
                        $scope.upload .error( errorFilePost);
                    }
                }
            });
    };
    function addCarouselItem (analysisId, newCarouselItem,name) {
        console.log("name",name);
        var analysis = $scope.profile.analyses.filter(function(x){return x.id === analysisId;})[0];
        analysis.efgsCarousel.push(newCarouselItem);
        $scope.electropherograms.push(newCarouselItem);
    }

    function sucessImagesPost(analysisId,filesId,name){
        return function(data){
            var newCarrouselItem = "/resources/temporary/" + data.substring(5);
                profileService.addElectropherograms(filesId, $scope.currentGlobalCode, analysisId,name).then(function(response) {
                    if (response.data) {
                        alertService.error({message: 'Error guardando las imagenes ' + response.data});
                    } else {
                        profileService.getElectropherogramsByAnalysisId($scope.currentGlobalCode,analysisId).then(function(response) {
                            var newArrray = response.data.map($scope.epgUploadedToModel);
                            var analysis = $scope.profile.analyses.filter(function(x){return x.id === analysisId;})[0];

                            for (var i = 0; i < newArrray.length; i++) {
                                if (analysis.electropherogramsList.filter(_.partial($scope.filterEmgById, newArrray[i].fileId)).length === 0){
                                    analysis.electropherogramsList.push(newArrray[i]);
                                }
                            }
                        });
                        alertService.success({message: 'Se insertaron las nuevas imagenes'});
                        addCarouselItem(analysisId, newCarrouselItem);
                    }
                });
            };
    }
    function sucessFilePost(analysisId,filesId,name){

        return function(data){
            console.log('sucessFilePost:data,analysisId,filesId,name',data,analysisId,filesId,name);
                profileService.addFiles(filesId, $scope.currentGlobalCode, analysisId,name).then(function(response) {
                    if (response.data) {
                        alertService.error({message: 'Error guardando el archivo: ' + response.data});
                    } else {
                        profileService.getFilesByAnalysisId($scope.currentGlobalCode,analysisId).then(function(response) {
                            var newArrray = response.data.map($scope.fileUploadedToModel);
                            var analysis = $scope.profile.analyses.filter(function(x){return x.id === analysisId;})[0];

                            analysis.fileList.splice(0,$scope.files.length);

                            for (var i = 0; i < newArrray.length; i++) {
                                analysis.fileList.push(newArrray[i]);
                                if ($scope.files.filter(_.partial($scope.filterFiles, newArrray[i].path)).length === 0){
                                    $scope.files.push(newArrray[i]);
                                }
                            }
                        });
                        alertService.success({message: 'Se insertó el nuevo archivo'});
                    }
                });
            };
        }
    var errorImagePost=function(){
        $log.log('error al mandar las imagenes');
    };
    var errorFilePost=function(){
        $log.log('error al mandar los archivos');
    };
}

return ProfileController;

});