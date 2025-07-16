/**
 * matcher controllers.
 */
define([ 'angular','lodash' ], function(angular,_) {
	'use strict';

	function ComparisonController(
		$scope,
		$routeParams,
		$modal,
		$timeout,
		$filter,
		$q,
		matcherService,
		profiledataService,
		profileService,
		$sce, 
		statsService,
		alertService,
		cryptoService,
		appConf,
		analysisTypeService,
		locusService,
		$window,
		notificationsService,
		shared
	) {
		$scope.lab = "-"+appConf.labCode+"-";
		$scope.stringency = matcherService.getStrigencyEnum();
		$scope.profileId = $routeParams.profileId;
		$scope.matchingId = $routeParams.matchingId;
		$scope.isPedigreeMatch = $routeParams.isPedigreeMatch === "true";
		$scope.matchedProfileId = $routeParams.matchedProfileId;
		$scope.isCollapsingMatch = $routeParams.isCollapsingMatch === "true";
		$scope.isScreening = $routeParams.isScreening === "true";
		$scope.trustAsHtml = $sce.trustAsHtml;
		$scope.selectedOptions = {
			'frequencyTable': null,
			'probabilityModel': null,
			'theta': null,
			'dropIn': null,
			'dropOut': null
		};
		$scope.stall = false;
		var modalInstanceHit = null;
		var modalInstanceEpg = null;
		$scope.associations = {};

		profileService.getStrKits().then(function (data) {
			$scope.strkits = data.data;
		});

		locusService.list().then(function(response) {
			$scope.locusById = {};
			response.data.forEach(function(l) {
				$scope.locusById[l.id] = l;
			});
		});
        
		$scope.showLocus = function(locus) {
			if ($scope.locusById && $scope.results) {
				return $scope.locusById[locus].analysisType === $scope.results.type;
			} else {
				return false;
			}
		};

		$scope.sortLoci = function (id) {
			profileService.getLociByStrKitId(id).then(function (response) {
				var order = {};
				response.data.forEach(function (elem) {
					order[elem.id] = elem.order;
				});
				$scope.comparision.sort(function (a, b) {
					if (order[a.locus] < order[b.locus]) {
						return -1;
					}
					if (order[a.locus] > order[b.locus]) {
						return 1;
					}
					return 0;
				});
			});
		};

		var giveModalHit = function (success, opt) {
			$scope.modalSuccess = success;
			$scope.opt = opt;
			modalInstanceHit = $modal.open({
				templateUrl: '/assets/javascripts/matches/views/hit-modal.html',
				scope: $scope
			});
		};

		$scope.closeModalEpg = function () {
			modalInstanceEpg.close();
		};

		$scope.closeModal = function () {
			modalInstanceHit.close();
		};

		function onResolution(response, status, action) {
			for (var i = 0; i < response.data.length; i++) {
				$scope.results.status[response.data[i]] = status;
			}
			var success = (response.data.length > 0);
			giveModalHit(success, action);
		}

		$scope.doDiscard = function (confirmRes) {
			if (!confirmRes) {
				return;
			}
			matcherService.doDiscard($scope.matchingId, $scope.profileId).then(
				function (response) {
					onResolution(response, 'discarded', 'descarte');
					getResults();
				},
				function (response) {
					alertService.error(response.data);
				});
		};

		$scope.doHit = function (confirmRes) {
			if (!confirmRes) {
				return;
			}
			matcherService.doHit($scope.matchingId, $scope.profileId).then(
				function (response) {
					onResolution(response, 'hit', 'acto de confirmación');
					getResults();
				},function (response) {
					alertService.error({message: response.data.message});
				}
				);
		};

		var loadCalculation = function() {
			$scope.showCalculation = $scope.analysisTypes[$scope.results.type].name === 'Autosomal' || $scope.fromDesktopSearch;
			if ($scope.showCalculation) {
				statsService
					.getDefaultOptions($scope.profileId)
					.then(
						function (opts) {
							$scope.selectedOptions = opts;
							getRandomMatchProbabilitiesByLocus();
						}, function () {
							statsService
								.getDefaultOptions($scope.matchedProfileId)
								.then(
									function (opts) {
										$scope.selectedOptions = opts;
										getRandomMatchProbabilitiesByLocus();
									});
						});
			}
		};

		var getResults = function() {
			if (!$scope.isPedigreeMatch) $scope.isPedigreeMatch = false;
			if (!$scope.isCollapsingMatch) $scope.isCollapsingMatch = false;
			if (!$scope.isScreening) $scope.isScreening = false;

			return matcherService
				.getResults(
					$scope.matchingId,
					$scope.isPedigreeMatch,
					$scope.isCollapsingMatch,
					$scope.isScreening
				).then(
				function (response) {
					if (
						response.data &&
						response.data.results &&
						response.data.results.length > 0
					) {
						$scope.results = response.data.results[0];
						console.log("resultMatch",$scope.results);
						$scope.matchingAlleles = Object
							.keys($scope.results.matchingAlleles)
							.map(
								function (value) { 
									return value.replace(",",".") ;
								}
							);
						var statusProfileId = $scope.results.status[$scope.profileId];
						var statusMatched = $scope.results.status[$scope.matchedProfileId];
						if(
							!_.isUndefined($scope.results.superiorProfile) &&
							$scope.results.superiorProfile!==null
						){
							$scope.assignProfile(
								$scope.results.superiorProfile.globalCode,
								$scope.results.superiorProfile
							);
							if(
								!_.isUndefined($scope.results.superiorProfileData) &&
								$scope.results.superiorProfileData!==null
							){
								if(
									$scope.results.superiorProfile.globalCode===$scope.profileId
								){
									$scope.profileData = {};
									$scope.assignProfileData(
										$scope.profileData,
										$scope.results.superiorProfileData,
										$scope.results.superiorProfile.globalCode
									);
								}
								if(
									$scope.results.superiorProfile.globalCode===$scope.matchedProfileId
								){
									$scope.matchedProfileData = {};
									$scope.assignProfileData(
										$scope.matchedProfileData,
										$scope.results.superiorProfileData,
										$scope.results.superiorProfile.globalCode
									);
								}
							}
						}
						$scope.closedMatch = true;
						if (statusProfileId === "discarded" && statusMatched === "discarded") {
							$scope.matchStatus = "discarded";
						} else if (statusProfileId === "hit" && statusMatched === "hit") {
							$scope.matchStatus = "hit";
						} else if (statusProfileId === "pending" || statusMatched === "pending") {
							$scope.matchStatus = "pending";
							$scope.closedMatch = false;
						} else {
							$scope.matchStatus = "conflict";
							$scope.closedMatch = false;
						}
						loadCalculation();
					} else {
						$scope.results = null;
					}
				}
			);
		};

		analysisTypeService.listById().then(function(response) {
			$scope.analysisTypes = response;
			getResults().then();
			//$scope.$apply();
		});
		profiledataService.getProfilesData([$scope.profileId, $scope.matchedProfileId]).then(
			function(response) {
				var profileDataTemp = response.data.filter(function(x){return x.globalCode === $scope.profileId;})[0];
					var matchedProfileDataTemp = response.data.filter(function(x){return x.globalCode === $scope.matchedProfileId;})[0];
				if(!_.isUndefined(profileDataTemp)){
					$scope.profileData = profileDataTemp;
				}
				if(!_.isUndefined(matchedProfileDataTemp)){
					$scope.matchedProfileData = matchedProfileDataTemp;
				}
				$scope.$apply();
			});
		
		$scope.labeledGenotypifications = {};
		$scope.labels = {};
		profileService.getProfile($scope.profileId).then(
			function(response) {
				$scope.assignProfile($scope.profileId,response.data);
				$scope.$apply();
			});
		profileService.getProfile($scope.matchedProfileId).then(
			function(response) {
				$scope.assignProfile($scope.matchedProfileId,response.data);
				$scope.$apply();
			}
		);
		$scope.assignProfile = function (profileId, profile){
			if (profile.labeledGenotypification) {
				$scope.labeledGenotypifications[profileId] = profile.labeledGenotypification;
				$scope.labels[profileId] = Object.keys(profile.labeledGenotypification);
			}
			$scope.labelSets = profile.labelsSets;
			$scope.mixM = profile.contributors > 1;
		};
		$scope.assignProfileData = function (localProfileData,superiorProfileData,globalCode) {
			if(!_.isUndefined(localProfileData) && !_.isUndefined(superiorProfileData) ){
				localProfileData.assignee = superiorProfileData.assignee;
				localProfileData.category = superiorProfileData.category;
				localProfileData.deleted = false;
				localProfileData.globalCode = globalCode;
				localProfileData.internalSampleCode = superiorProfileData.internalSampleCode;
				localProfileData.laboratory = superiorProfileData.laboratoryDescription;
				localProfileData.responsibleGeneticist = superiorProfileData.responsibleGeneticist;
				localProfileData.bioMaterialType = superiorProfileData.bioMaterialType;
				localProfileData.sampleEntryDate = superiorProfileData.sampleEntryDate;
				localProfileData.sampleDate = superiorProfileData.sampleDate;
				localProfileData.profileExpirationDate = superiorProfileData.profileExpirationDate;
				$scope.$apply();
			}
		};

		$scope.setComparisions = function() {
			if (!$scope.isCollapsingMatch) $scope.isCollapsingMatch = false;
			if (!$scope.isScreening) $scope.isScreening = false;

			return matcherService
				.getComparedGenotyfications(
					$scope.profileId,
					$scope.matchedProfileId,
					$scope.matchingId,
					$scope.isCollapsingMatch,
					$scope.isScreening
				).then(
				function (response) {
					function mtConvert(item) {
						item.locusSort = item.locus;
						if (item.locus === 'HV1') {
							item.locusSort = 'HV1_VAR';
						}
						if (item.locus === 'HV2') {
							item.locusSort = 'HV2_VAR';
						}
						if (item.locus === 'HV3') {
							item.locusSort = 'HV3_VAR';
						}
						if (item.locus === 'HV4') {
							item.locusSort = 'HV4_VAR';
						}
						return item;
					}

					$scope.comparision = _.sortBy(response.data.map(mtConvert), ['locusSort']);
					console.log('comparision', $scope.comparision);
					$scope.$apply();
				}
			);
		};

		$scope.setComparisions().then();
		function encryptedEpgs(profile, epgs) {
			return epgs.map(function(e){
				return cryptoService.encryptBase64("/profiles/" + profile + "/epg/" + e.fileId);
			});
		}
		
		profileService.getElectropherogramsByCode($scope.profileId).then(
			function(response) {
				$scope.epg = encryptedEpgs($scope.profileId, response.data);
			});
	
		profileService.getElectropherogramsByCode($scope.matchedProfileId).then(
			function(response) {
				$scope.matchedepg = encryptedEpgs($scope.matchedProfileId, response.data);
			});

		$scope.printReport = function() {
			var head = '<head><title>Comparación</title>';
			$("link").each(function () {
				head += '<link rel="stylesheet" href="' + $(this)[0].href + '" />';
			});
			head += "</head>";
			$scope.$apply();
			var report = window.open('', '_blank');
			report.document.write(
				'<html>' + head +
				'<body>' +
				$('#report').html() +
				'</body></html>'
			);
			report.document.close();
			$(report).on('load', function(){
				report.print();
				report.close();
			});
		};
		
		profiledataService.getCategories().then(function(response){
			$scope.categories = response.data;
		});
		
		$scope.getSubcatName2 = function(catId){
			return matcherService.getSubCatName($scope.categories, catId);
		};
		
		$scope.getSubcatName = function(catId){
			return matcherService.getSubCatName($scope.categories, catId);
		};
        $scope.backCollapsing = function(){
            $window.history.back();
        };
		$scope.showElectropherograms = function(){
			modalInstanceEpg = $modal.open({
				templateUrl:'/assets/javascripts/matches/views/electropherograms-modal.html',
				scope: $scope
			});
		};
		
		$scope.getStatsInfo = function(){
			if (!$scope.selectedOptions){return;}
			
			var statHeader = '<div class="form-group"><label>lblTitle:</label> ';
			var statFooter = '</div>';
			var selOpt = $scope.selectedOptions;
			return statHeader.replace('lblTitle', 'Base de datos de frecuencia') +
				((selOpt.frequencyTable)? selOpt.frequencyTable: '')  + statFooter + 
				statHeader.replace('lblTitle', 'Modelo estadístico') +
				((selOpt.probabilityModel)? selOpt.probabilityModel: '') + statFooter + 
				statHeader.replace('lblTitle', '&Theta;') + 
				((selOpt.theta)? selOpt.theta: '') + statFooter;
		};
		
		function getRandomMatchProbabilitiesByLocus() {
			if(
				!$scope.selectedOptions ||
				!$scope.selectedOptions.frequencyTable ||
				!$scope.selectedOptions.probabilityModel ||
				$scope.selectedOptions.dropIn === undefined ||
				$scope.selectedOptions.dropOut === undefined ||
				$scope.selectedOptions.theta === undefined
			) {
				alertService.info({message: 'No existen parámetros para el cálculo del LR'});
				return;
			}
			matcherService.getLR(
					$scope.profileId,
					$scope.matchedProfileId,
					$scope.matchingId,
					$scope.selectedOptions)
				.then(function(response) {
						$scope.statsResolved = response.data.detailed;
						$scope.pvalue = response.data.total;
					});
		}
		
		$scope.showStatsOptions = function(){
			var modalStatInstance = $modal.open({
				templateUrl:'/assets/javascripts/matches/views/stats-option-modal.html',
				controller: 'statsOptionModalController',
				resolve: {
					selectedOptions: function() {
						return $scope.selectedOptions;
					},
          mix: function() {
              return $scope.mixF && $scope.mixM;
          },
          profileData: function() {
              var obj = {};
              obj[$scope.profileId] = $scope.profileData;
              obj[$scope.matchedProfileId] = $scope.matchedProfileData;
              return obj;
          }
				}
			});
			modalStatInstance.result.then(
				function (statsOptions) {
					$scope.selectedOptions = statsOptions;
					getRandomMatchProbabilitiesByLocus();
				}, 
				function () {//dismissed
				}
			);
		};
        $scope.shouldShowMaxAlelle = locusService.shouldShowMaxAlelle;
        $scope.shouldShowMinAlelle = locusService.shouldShowMinAlelle;

		$scope.setLabel = function(p, locus, allele, associated) {
			if (associated) {
				return $scope.labeledGenotypifications[p] && $scope.labels[p] && $scope.labeledGenotypifications[p][associated] &&
					$scope.labeledGenotypifications[p][associated][locus] &&
					$scope.labeledGenotypifications[p][associated][locus].indexOf(allele) > -1;
			} else {
				return $scope.labeledGenotypifications[p] && $scope.labels[p] && $scope.labeledGenotypifications[p][$scope.labels[p][0]][locus] &&
					$scope.labeledGenotypifications[p][$scope.labels[p][0]][locus].indexOf(allele) > -1;
			}
		};

        $scope.getLabelCaption = function(label) {
            for (var id in $scope.labelSets) {
                var elements = $scope.labelSets[id];
                if (elements[label]) {
                    return elements[label].caption;
                }
            }
            return '';
        };
        
        $scope.getProfileLabelCaption = function(globalCode, item) {
            var lab = globalCode.split("-")[2];
            if(lab === appConf.labCode){
                profileService.getProfile(globalCode).then(
                    function(response) {
                        $scope.associations[item] = globalCode + " (" + response.data.profileData.internalSampleCode + ")";
                    });
            } else {
                $scope.associations[item] = globalCode;
            }
        };
        
        $scope.checkLabel = function (p) {
            return $scope.labels[p] && ["1","2","3","4"].indexOf($scope.labels[p][0]) === -1;
        };

		$scope.getAnalysisName = function (item){
			if (item && $scope.analysisTypes) {
				return $scope.analysisTypes[item.type].name;
			}
		};

		function cantidadDeContributors() {
					profileService.findByCode($scope.profileId).then(function (result) {
						$scope.profileIContributors = result.data.contributors;
					profileService.findByCode($scope.matchedProfileId).then(function (result) {
						$scope.matchingIContributors = result.data.contributors;
					if(( $scope.profileIContributors === 2 && $scope.matchingIContributors > 2 ) ||
						($scope.profileIContributors > 2 && $scope.matchingIContributors === 2)) {
						$scope.matchAg = true;
					}else{
						$scope.matchAg =false;
					}
				});
			});
		}
		cantidadDeContributors();

		// for desktop search

		notificationsService.onMatchStatusNotification(function(msg){
			var status = msg.status;
			if (status === "started"){
				$scope.stall = false;
				$scope.working = true;
				$scope.fail = false;
				console.log("Match started");
			}else if (status === "ended"){
				$scope.stall = true;
				$scope.working = false;
				$scope.fail = false;
				console.log("Match ended");
			}else if (status === "fail"){
				$scope.stall = false;
				$scope.working = false;
				$scope.fail = true;
				console.log("Match failed");
			}else if (status === "pedigreeStarted"){
				$scope.pedigreeStall = false;
				$scope.pedigreeWorking = true;
				console.log("Pedigree Match started");
			} else if (status === "pedigreeEnded"){
				$scope.pedigreeStall = true;
				$scope.pedigreeWorking = false;
				console.log("Pedigree Match ended");
			}
			$scope.profileId = shared.profileId;
			$scope.profileData = shared.profileData;
			$scope.matches = shared.matches;
			$scope.$apply();

		});

		$scope.hasMatches = function(){
			var length = Object.keys($scope.matches).length;
			return length > 0;
		};

		$scope.printDesktopSearchReport = function(matchedProfileId) {
			$scope.showCalculation = true;
			$scope.matchedProfileId = matchedProfileId;
			console.debug("Al momento de imprimir el reporte, $scope.profileData es: ", $scope.profileData);
			$scope.matchingId = $scope.matches[matchedProfileId];

			profiledataService.getProfileDataBySampleCode(matchedProfileId).then(function (response) {
				$scope.matchedProfileData = response.data;
				console.debug("Obteniendo el tipo de análisis para el reporte.");
				var head = '<head><title>Comparación</title>';
				$("link").each(function () {
					head += '<link rel="stylesheet" href="' + $(this)[0].href + '" />';
				});
				head += "</head>";
				analysisTypeService.listById().then(function(response) {
					$scope.analysisTypes = response;
					statsService
						.getDefaultOptions($scope.profileId)
						.then(
							function (opts) {
								$scope.selectedOptions = opts;
								console.debug("Preparando comparación para impresión.");
								$q.all([
									$scope.setComparisions(),
									getResults(),
									matcherService.getLR(
										$scope.profileId,
										$scope.matchedProfileId,
										$scope.matchingId,
										$scope.selectedOptions)
								]).then(function (responses) {
									var lrResponse = responses[2];
									$scope.statsResolved = lrResponse.data.detailed;
									$scope.pvalue = lrResponse.data.total;
									$scope.$apply();
									console.debug("Imprimiendo reporte");
									var report = window.open('', '_blank');
									report.document.write(
										'<html>' + head +
										'<body>' +
										$('#report').html() +
										'</body></html>'
									);
									report.document.close();
									$(report).on('load', function () {
										report.print();
										report.close();
									});
								});
							});
				});
			});
		};


		$scope.cancel = function () {
			$scope.$dismiss('cancel');
		};


	}
	return ComparisonController;
});
