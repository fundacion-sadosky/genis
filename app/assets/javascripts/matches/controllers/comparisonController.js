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
		$window
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
      return true; // for debugging
      // if ($scope.locusById && $scope.results) {
      //     return $scope.locusById[locus].analysisType === $scope.results.type;
      // } else {
      //     return false;
      // }
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
			$scope.showCalculation = $scope.analysisTypes[$scope.results.type].name === 'Autosomal';
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
			matcherService
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
			getResults();
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
		});
		
		$scope.labeledGenotypifications = {};
		$scope.labels = {};
		profileService.getProfile($scope.profileId).then(
			function(response) {
				$scope.assignProfile($scope.profileId,response.data);
		});
		profileService.getProfile($scope.matchedProfileId).then(
			function(response) {
				$scope.assignProfile($scope.matchedProfileId,response.data);
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
			}
		};
		
		matcherService
			.getComparedGenotyfications(
				$scope.profileId,
				$scope.matchedProfileId,
				$scope.matchingId,
				$scope.isCollapsingMatch,
				$scope.isScreening
			).then(
				function(response) {
					function mtConvert(item) {
						item.locusSort = item.locus;
						if(item.locus === 'HV1'){
								item.locusSort = 'HV1_VAR';
						}
						if(item.locus === 'HV2'){
								item.locusSort = 'HV2_VAR';
						}
						if(item.locus === 'HV3'){
								item.locusSort = 'HV3_VAR';
						}
						if(item.locus === 'HV4'){
								item.locusSort = 'HV4_VAR';
						}
						return item;
					}
					$scope.comparision = _.sortBy(response.data.map(mtConvert), ['locusSort']);
					console.log('comparision',$scope.comparision);
			}
		);
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
			var rowBackground = true;
			var createEmptyReport = function (){
				var newWindow = window.open('', '_blank');
				newWindow.document.write('<html><head><title></title></head><body></body></html>');
				newWindow.document.close();
				return newWindow;
			};
			var setHeadAndBodyStructure = function(doc) {
				doc.title = "GENIS - Reporte de coincidencias";
				var $body = $('body', doc);
				$body.append('<h1 id="reportTitle">Reporte de Coincidencias</h1>');
				var $title = $('#reportTitle', doc);
				$body.css("font-family", "Helvetica");
				$title.css("text-align", "center");
				$("head", doc).append(
					'<style>'+
					'.key{text-align:right; width:35%; font-weight: bold; font-size: small}'+
					'.val{text-align:right; width:65%;}'+
					'.m1key{text-align:right; width:30%; font-weight: bold; font-size: small}'+
					'.m2key{text-align:right; width:30%}'+
					'.mval{text-align:right; width:40%;}'+
					'.crkey{text-align: right; width: 40%; font-weight: bold; font-size: small;}'+
					'.crval{text-align: right; width: 30%;}'+
					'.rowSmall{font-size: x-small;}'+
					'.lbg{background-color: #f0f0f0;}'+
					'.dbg{background-color: #F5F5F5;}'+
					'#summary{width:100%;}'+
					'#mito{width:100%;}'+
					'#cromosomal{width:100%;}'+
					'.summTitle{' +
					'  text-align:right;' +
					'  font-weight: bold;' +
					'  border-bottom-style: solid;' +
					'  border-bottom-color: silver;' +
					'  border-bottom-width: 2px;}'+
					'</style>'
				);
				$body.append('<table id="summary">');
			};
			var addSummaryTitle = function(doc, text) {
				var summ = $('#summary', doc);
				summ.append('<tr>');
				$('#summary tr:last', doc)
					.append('<td class="summTitle" colspan="2"><div>'+text+'</div></td>');
			};
			var addSummaryRowGeneric = function(
				doc,
				keyColText,
				valueColText,
				keyClasses,
				valClasses
			) {
				if (valueColText === undefined || valueColText === null) {
					rowBackground = !rowBackground;
					return;
				}
				var summ = $('#summary', doc);
				summ.append('<tr>');
				$('#summary tr:last', doc)
					.append('<td class="'+keyClasses+'"><div>'+keyColText+':</div></td>');
				$('#summary tr:last', doc)
					.append('<td class="'+valClasses+'"><div>'+valueColText+'</div></td>');
			};
			var addRangeRowGeneric = function(
				doc,
				title,
				keyColText,
				valueColText,
				key1Classes,
				key2Classes,
				valClasses
			) {
				if (valueColText === undefined || valueColText === null) {
					rowBackground = !rowBackground;
					return;
				}
				var summ = $('#mito', doc);
				summ.append('<tr>');
				$('#mito tr:last', doc)
					.append('<td class="'+key1Classes+'"><div>'+title+'</div></td>');
				$('#mito tr:last', doc)
					.append('<td class="'+key1Classes+'"><div>'+keyColText+'</div></td>');
				$('#mito tr:last', doc)
					.append('<td class="'+valClasses+'"><div>'+valueColText+'</div></td>');
			};
			var addSummarySpacerRow = function(doc) {
				var summ = $('#summary', doc);
				summ.append('<tr>');
				$('#summary tr:last', doc)
					.append('<td class="key rowSmall"><div> </div></td>');
				$('#summary tr:last', doc)
					.append('<td class="val rowSmall"><div> </div></td>');
			};
			var addEndOfSummarySpacer = function(doc) {
				$('#summary', doc).after('<div><br></div>');
			};
			var addSummaryRow = function(doc, keyColText, valueColText) {
				var bgClass = rowBackground ? "lbg" : "dbg";
				rowBackground = !rowBackground;
				addSummaryRowGeneric(doc, keyColText, valueColText, "key " + bgClass, "val " + bgClass);
			};
			var addSummaryRowSmall = function(doc, keyColText, valueColText) {
				var bgClass = rowBackground ? "lbg" : "dbg";
				rowBackground = !rowBackground;
				addSummaryRowGeneric(
					doc, keyColText, valueColText, "key rowSmall "+ bgClass, "val rowSmall " + bgClass
				);
			};
			var addMitoStruct = function(doc) {
				var $body = $('body', doc);
				$body.append('<table id="mito">');
			};
			var addMitoTitle = function(doc, title) {
				var summ = $('#mito', doc);
				summ.append('<tr>');
				$('#mito tr:last', doc)
					.append('<td class="summTitle" colspan="3"><div>' + title + '</div></td>');
			};
			var addMitoRangeRow = function(doc, title, keyColText, valueColText) {
				var bgClass = rowBackground ? "lbg" : "dbg";
				rowBackground = !rowBackground;
				addRangeRowGeneric(
					doc,
					title,
					keyColText,
					valueColText, 
					"m1key " + bgClass,
					"m2key " + bgClass,
					"mval " + bgClass
				);
			};
			var addEndOfMitoSpacer = function(doc) {
				$('#mito', doc).after('<div><br></div>');
			};
			var addCromosomalStruct = function(doc) {
				var $body = $('body', doc);
				$body.append('<table id="cromosomal">');
			};
			var addCromosomalTitle = function(doc, title) {
				var cr = $('#cromosomal', doc);
				cr.append('<tr>');
				$('#cromosomal tr:last', doc)
					.append('<td class="summTitle" colspan="3"><div>' + title + '</div></td>');
			};
			var addCromosomalProfilesNames = function(doc, profile1, profile2) {
				var bgClass = rowBackground ? "lbg" : "dbg";
				rowBackground = !rowBackground;
				var cr = $('#cromosomal', doc);
				cr.append('<tr>');
				$('#cromosomal tr:last', doc)
					.append('<td class="crkey '+bgClass+'"><div>Perfiles:</div></td>');
				$('#cromosomal tr:last', doc)
					.append('<td class="crval '+bgClass+'"><div>' + profile1 + '</div></td>');
				$('#cromosomal tr:last', doc)
					.append('<td class="crval '+bgClass+'"><div>' + profile2 + '</div></td>');
			};
			var addCromosomalMarkers = function(doc, comparision, pid1, pid2) {
				// Get marker names
				// create a mapping from marker to allele for each profile
				// Select sex marker
				// Sort autosomal markers by name
				// Show markers
				var allMarkers = comparision
					.map(function(x) {return x.locus;})
					.filter(function(x) {return !x.startsWith("HV");});
				var markerMap = new Map(
					comparision
						.filter(function(x) {return !x.locus.startsWith("HV");})
						.map(
							function(x) {
								return [x.locus, new Map(Object.entries(x.g))];
							}
						)
				);
				var sexMarker = Array.from(markerMap.entries())
					.map(
						function(k){
							return [
								k[0],
								Array.from(k[1].entries())
									.map(function(x){return x[1];})
									.reduce(function(a, x) {return a.concat(x);})
							];
						}
					)
					.map(function(x){return [x[0], new Set(x[1])];})
					.filter(function(x) {return x[1].size <=1 && (x[1].has("X") || x[1].has("Y"));})
					.map(function(x){return x[0];})[0];
				var autosomalMarkers = allMarkers
					.filter(function (x) {return x !== sexMarker;})
					.sort();
				allMarkers = [sexMarker].concat(autosomalMarkers);
				for (var i in allMarkers) {
					var cMarker = allMarkers[i];
					if (cMarker === undefined) {
						continue;
					}
					var bgClass = rowBackground ? "lbg" : "dbg";
					rowBackground = !rowBackground;
					var cr = $('#cromosomal', doc);
					cr.append('<tr>');
					$('#cromosomal tr:last', doc)
						.append('<td class="crkey '+bgClass+'"><div>'+cMarker+'</div></td>');
					$('#cromosomal tr:last', doc)
						.append('<td class="crval '+bgClass+'"><div>' + markerMap.get(cMarker).get(pid1) + '</div></td>');
					$('#cromosomal tr:last', doc)
						.append('<td class="crval '+bgClass+'"><div>' + markerMap.get(cMarker).get(pid2) + '</div></td>');
				}
			};
			var selectRanges = function(comparision, profile) {
				var ranges = comparision
					.filter(function(x) {return x.locus.endsWith("_RANGE");})
					.map(function(x) {return x.g[profile];})
					.filter(function(x) {return x !== undefined;});
				return ranges;
			};
			var rangeAsString = function(range) {
				if (range === undefined) {
					return "";
				}
				if (range.length === 2) {
					return "[" + range[0] + " - " + range[1] + "]";
				}
				return "";
			};
			var addDefinedRanges = function(doc, comparision, profileId, matchedProfileId) {
				var pRanges = selectRanges(comparision, profileId);
				var mRanges = selectRanges(comparision, matchedProfileId);
				var title = "Rangos Definidos:";
				var pid = profileId;
				for (var i in pRanges) {
					addMitoRangeRow(doc, title, pid, rangeAsString(pRanges[i]));
					pid = "";
					title = "";
				}
				pid = matchedProfileId;
				for (i in mRanges) {
					addMitoRangeRow(doc, title, pid, rangeAsString(mRanges[i]));
					pid = "";
				}
			};
			var selectMatchingAlleles = function(comparision, profile, matching) {
				var mAlleles = comparision
					.filter(function(x){return x.locus.startsWith("HV") && !x.locus.endsWith("_RANGE");})
					.flatMap(function(x){return x.g[profile];})
					.filter(function(x){return x!==undefined;})
					.filter(function(x){return matching.includes(x);});
				return mAlleles;
			};
      var selectNonMatchingAlleles = function(comparision, profile, matching) {
        var mAlleles = comparision
          .filter(function(x){return x.locus.startsWith("HV") && !x.locus.endsWith("_RANGE");})
          .flatMap(function(x){return x.g[profile];})
          .filter(function(x){return x!==undefined;})
          .filter(function(x){return !matching.includes(x);});
        return mAlleles;
      };
			var addMathingAlleles = function(doc, comparision, profileId, matchedProfileId, matching) {
				var pAlleles = selectMatchingAlleles(comparision, profileId, matching);
				var mAlleles = selectMatchingAlleles(comparision, matchedProfileId, matching);
				var title = "Alelos coincidentes:";
				var pid = profileId;
				var allele = "";
				for (var i in pAlleles) {
					allele = $filter("mt")(pAlleles[i], "", $scope.results.type, $scope.analysisTypes);
					addMitoRangeRow(doc, title, pid, allele);
					pid = "";
					title = "";
				}
				pid = $scope.matchedProfileId;
				for (i in mAlleles) {
					allele = $filter("mt")(mAlleles[i], "", $scope.results.type, $scope.analysisTypes);
					addMitoRangeRow(doc, title, pid, allele);
					pid = "";
				}
			};
			var addNonMathingAlleles = function(doc, comparision, profileId, matchedProfileId, matching) {
        var pAlleles = selectNonMatchingAlleles(comparision, profileId, matching);
        var mAlleles = selectNonMatchingAlleles(comparision, matchedProfileId, matching);
        var title = "Alelos no coincidentes:";
        var pid = profileId;
        var allele = "";
        for (var i in pAlleles) {
          allele = $filter("mt")(pAlleles[i], "", $scope.results.type, $scope.analysisTypes);
          addMitoRangeRow(doc, title, pid, allele);
          pid = "";
          title = "";
        }
        pid = $scope.matchedProfileId;
        for (i in mAlleles) {
          allele = $filter("mt")(mAlleles[i], "", $scope.results.type, $scope.analysisTypes);
          addMitoRangeRow(doc, title, pid, allele);
          pid = "";
        }
			};
			var addTotalNumberOfDifferences = function(doc, comparision, profileId, matchedProfileId, matching) {
				// TODO: Check if this function works well with insertions and deletions
				var pAlleles = selectNonMatchingAlleles(comparision, profileId, matching);
				var mAlleles = selectNonMatchingAlleles(comparision, matchedProfileId, matching);
				var allAlleles = pAlleles.concat(mAlleles);
				var nDiff = new Set(allAlleles.map(function(x) {return x.split("@")[1];})).size;
				addMitoRangeRow(doc, "Total de diferencias:", "", nDiff);
			};
			$timeout(function(){
				var report = createEmptyReport();
				$(report.document).ready(
					function() {
						setHeadAndBodyStructure(report.document);
						addSummaryTitle(report.document, "Resumen de perfiles");
						addSummarySpacerRow(report.document);
						addSummaryRow(report.document, "Codigo Genis", $scope.profileId);
						addSummaryRow(report.document, "Codigo Laboratorio", $scope.profileData.internalSampleCode);
						addSummaryRow(report.document, "Categoria", $scope.getSubcatName($scope.profileData.category));
						addSummaryRowSmall(report.document, "Gen. Asignado", $scope.profileData.assignee);
						addSummaryRowSmall(report.document, "Gen. Responsable", $scope.profileData.responsibleGeneticist);
						addSummaryRowSmall(report.document, "Fecha de caducidad del perfil", $scope.profileData.profileExpirationDate);
						addSummaryRowSmall(report.document, "Laboratorio", $scope.profileData.laboratory);
						addSummaryRowSmall(report.document, "Tipo De Muestra Biologica", $scope.profileData.bioMaterialType);
						addSummaryRowSmall(report.document, "Fecha De Ingreso", $scope.profileData.sampleEntryDate);
						addSummaryRowSmall(report.document, "Fecha Toma de Muestra", $scope.profileData.sampleDate);
						addSummaryRowSmall(
							report.document,
							"Estado",
							$filter('translatematchstatus')($scope.results.status[$scope.profileId].toUpperCase())
						);
						addSummarySpacerRow(report.document);
						addSummaryRow(report.document, "Codigo Genis", $scope.matchedProfileId);
						addSummaryRow(report.document, "Codigo Laboratorio", $scope.matchedProfileData.internalSampleCode);
						addSummaryRow(report.document, "Categoria", $scope.getSubcatName($scope.matchedProfileData.category));
						addSummaryRowSmall(report.document, "Gen. Asignado", $scope.matchedProfileData.assignee);
						addSummaryRowSmall(report.document, "Gen. Responsable", $scope.matchedProfileData.responsibleGeneticist);
						addSummaryRowSmall(report.document, "Fecha de caducidad del perfil", $scope.matchedProfileData.profileExpirationDate);
						addSummaryRowSmall(report.document, "Laboratorio", $scope.matchedProfileData.laboratory);
						addSummaryRowSmall(report.document, "Tipo De Muestra Biologica", $scope.matchedProfileData.bioMaterialType);
						addSummaryRowSmall(report.document, "Fecha De Ingreso", $scope.matchedProfileData.sampleEntryDate);
						addSummaryRowSmall(report.document, "Fecha Toma de Muestra", $scope.matchedProfileData.sampleDate);
						addSummaryRowSmall(
							report.document,
							"Estado",
							$filter('translatematchstatus')($scope.results.status[$scope.matchedProfileId].toUpperCase())
						);
						if ($scope.showCalculation) {
							addSummarySpacerRow(report.document);
							addSummaryTitle(report.document, "Estadística");
							addSummarySpacerRow(report.document);
							addSummaryRow(
								report.document,
								"LR",
								$filter('likelihoodratioComp')($scope.pvalue, true, $scope.statsResolved)
							);
							var selOpt = $scope.selectedOptions;
							addSummaryRowSmall(
								report.document,
								"Base de datos de frecuencia",
								(selOpt.frequencyTable)? selOpt.frequencyTable: '-'
							);
							addSummaryRowSmall(
								report.document,
								"Modelo estadístico",
								(selOpt.probabilityModel)? selOpt.probabilityModel: '-'
							);
							addSummaryRowSmall(
								report.document,
								"Valor &Theta;",
								(selOpt.theta)? selOpt.theta: '-'
							);
							addSummaryRowSmall(
								report.document,
								"Estrictez",
								$scope.stringency[$scope.results.stringency].text
							);
							var alleles = $scope.results.reducedStringencies.values()[0];
							addSummaryRowSmall(
								report.document,
								"Alelos",
								alleles + ' / ' + $scope.results.totalAlleles
							);
						}
						addEndOfSummarySpacer(report.document);
						if ($scope.results.type === 4) { // Mitochondrial
							addMitoStruct(report.document);
							addMitoTitle(report.document, "Análisis mitocondrial");
							addDefinedRanges(
								report.document,
								$scope.comparision,
								$scope.profileId,
								$scope.matchedProfileId
							);
							addMathingAlleles(
								report.document,
								$scope.comparision,
								$scope.profileId,
								$scope.matchedProfileId,
								$scope.matchingAlleles
							);
							addNonMathingAlleles(
								report.document,
								$scope.comparision,
								$scope.profileId,
								$scope.matchedProfileId,
								$scope.matchingAlleles
							);
							addTotalNumberOfDifferences(
								report.document,
								$scope.comparision,
								$scope.profileId,
								$scope.matchedProfileId,
								$scope.matchingAlleles
							);
							addEndOfMitoSpacer(report.document);
						}
						console.log($scope.showCalculation);
						if ($scope.showCalculation || !$scope.showCalculation) { // All autosomal alleles
							addCromosomalStruct(report.document);
							addCromosomalTitle(report.document, "Análisis de marcadores cromosómicos");
							addCromosomalProfilesNames(
								report.document,
								$scope.profileId,
								$scope.matchedProfileId
							);
							addCromosomalMarkers(
								report.document,
								$scope.comparision,
								$scope.profileId,
								$scope.matchedProfileId
							);
						}
						// newWindow.print();
						// newWindow.close();
				});
			});
		};
		
		profiledataService.getCategories().then(function(response){
			$scope.categories = response.data;
		});
		
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
	}
	return ComparisonController;
});
