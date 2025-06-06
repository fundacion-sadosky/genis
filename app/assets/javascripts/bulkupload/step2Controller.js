define(['jquery','lodash'], function($,_) {
	'use strict';

	function Step2Controller($scope, $routeParams, bulkuploadService, helper, $log, $modal, alertService, $q, userService, locusService, profiledataService, notificationsService, matchesService) {

		$scope.statusMap = bulkuploadService.getStatusMap();
		var toogle = true;
        var user = userService.getUser();

        localStorage.removeItem("searchPedigree");
        localStorage.removeItem("searchMatches");
        localStorage.removeItem("searchPedigreeMatches");
		
		var $modalInstance = {};
        $scope.editedSubcats = {};
        $scope.batches = [];
        $scope.protoProfiles = {};
        $scope.showOptions = [{label: 'Pendientes', filters: {pending: true}, placement: 'bottom'},
            {label: 'Todo', filters: {}, placement: 'top'}];
        $scope.activeOption = 1;

            bulkuploadService.getSubcategories().then(function(response){
            $scope.subcategory = response.data;
		} );
        profiledataService.getCategories().then(function(response) {
            $scope.categories = response.data;
        });
        var getBatchProtoProfiles = function(batch) {
            batch.isProcessing = true;
            return bulkuploadService.getProtoProfilesStep2(user.geneMapperId, batch.id, batch.page - 1, batch.pageSize).then(function(response) {
                for(var i = 0; i < response.data.length; i++){
                    if($scope.subcategory[response.data[i].category].replicate && response.data[i].status === "Approved"){
                        response.data[i].replicate = false;
                        response.data[i].replicateDisabled =  false;
                    }else{
                        response.data[i].replicate = false;
                        response.data[i].replicateDisabled =  true;
                    }
                    _.forEach(response.data[i].genotypification,_.partial(bulkuploadService.fillRange,$scope.locusById,locusService.isOutOfLadder));
                }
				$scope.protoProfiles[batch.id] = response.data;
               aprobados(batch.id);
                batch.isProcessing = false;
            }, function() { batch.isProcessing = false; });
        };
        var loadLocus = function() {
            return locusService.listFull().then(function(response) {
                $scope.locus = response.data;
                $scope.locusById = _.keyBy(_.map($scope.locus, function (o) {  return o.locus;}), 'id');
            });
        };
        var getAllBatches = function() {
            $scope.isProcessing =true;
            return bulkuploadService.getBatchesStep2(user.geneMapperId).then(function(response) {
                if( $scope.activeOption === 0){
                    $scope.batches = response.data.filter(function (t) {
                        return t.totalForApprovalOrImport !== 0;
                    });
                }else {
                    $scope.batches = response.data;
                }
                $scope.batches.forEach(function(batch){
                    batch.pageSize = 50;
                    batch.page = 1;
                    var isOpen = false;

                    Object.defineProperty(batch, "isOpen",
                        {
                            get : function(){ return isOpen; },
                            set : function(newValue) {
                                isOpen = newValue;
                                if (isOpen) {
                                    getBatchProtoProfiles(batch);
                                }
                            }});
                });
                $scope.isProcessing = false;
            });
        };

        $scope.changePage = function(batch) {
            getBatchProtoProfiles(batch);
        };
		
		var getBatchItem = function(id, batch) {
			if (batch){
				batch.isProcessing = true;
			}

			bulkuploadService.getProtoProfileBySampleId(id).then(function(response) {		
				if (batch && batch.id) {
					$scope.protoProfiles[batch.id] = $scope.protoProfiles[batch.id].map(function(x){
						return (x.id === id)? response.data: x;
					});
				} else {
                    $scope.protoProfiles = helper.groupBy([response.data], 'batchId');
                    $scope.batches = Object.keys($scope.protoProfiles).map(function(id) {
                        return {id: id, isOpen: true};
                    });
				}
				if (batch){
                    batch.isProcessing = false;
				}
			}, function() { if (batch) { batch.isProcessing = false; } });
		};

        $scope.protoprofileId = $routeParams.protoprofileId;
        if ($scope.protoprofileId) {
            getBatchItem($scope.protoprofileId, undefined);
        } else {
            getAllBatches();
            loadLocus();
        }
		
		var updateStatus = function(sample, status, batch) {
			var id = parseInt(sample.id);
            batch.isProcessing = true;
            return  bulkuploadService.changeStatus(id, status,sample.replicate).then(function(response) {
				if(response.data.length > 0) {
					if (batch.id) {
						$scope.protoProfiles[batch.id].forEach(function(b){
							if (b.id === id) {b.errors = response.data;}
						});
                    } else { $scope.protoProfiles[0].errors = response.data; }
				}else { sample.status = status; }
                    aprobados(batch.id);
                    batch.isProcessing = false;
			},
                function(response){
//				alert('Hubo un error: ' + response.data);
                batch.isProcessing = false;
				alertService.error({message: ' Hubo un error: ' + response.data});
				$log.log(response);
			});
		};
		
		var rejectPp = function(sample, motive, batch,idMotive) {
            batch.isProcessing = true;
            bulkuploadService.rejectProtoProfile(sample.id, motive,idMotive).then(function(response) {
				if(response.data.length > 0) {
					if (batch) {
						$scope.protoProfiles[batch.id].forEach(function(b){
							if (b.id === sample.id) {b.errors = response.data;}
						});
					} else { $scope.protoProfiles[0].errors = response.data; }
				}
				else { sample.status = 'Rejected';
                    sample.selected = false;
				}
                    aprobados(batch.id);
                    batch.isProcessing = false;
                },
			function(response){
//				alert('Hubo un error: ' + response.data);
                batch.isProcessing = false;
                alertService.error({message: ' Hubo un error: ' + response.data});
				$log.log(response);
			});
		};
		
		$scope.import = function(id, batch) {
			updateStatus(id, 'Imported', batch);
		};
		$scope.onChangeMotive = function(selectedMotive){

            $scope.showMotiveTextArea = false;
            for (var i = 0; i < $scope.motives.length; i++) {
                if($scope.motives[i].id === parseInt(selectedMotive) && $scope.motives[i].freeText){
                    $scope.showMotiveTextArea = true;
                    $scope.motiveText = "";
                }
            }

		};
		$scope.reject = function(sample, batchId) {
            $scope.showMotiveTextArea = false;
			console.log(batchId);
            bulkuploadService.getMotives().then(function(response) {
                $scope.motives = response.data;
            }, function() {
                $scope.motives = [];
            });
			$modalInstance = $modal.open({
				templateUrl:'/assets/javascripts/bulkupload/modalRejectProtoProfile.html',
				scope: $scope
			});
			
			$modalInstance.result.then(function(res){
				rejectPp(sample,res.motive,batchId,res.idMotive);
			});
			
			//rejectProtoProfile(id, '');
		};
		
		$scope.closeModal = function(motiveText,selectedMotive){
			if(motiveText === false){
				$modalInstance.dismiss('cancel');
			}else{
                for (var i = 0; i < $scope.motives.length; i++) {
                    if($scope.motives[i].id === parseInt(selectedMotive) && !$scope.motives[i].freeText){
                        motiveText = $scope.motives[i].description;
                    }
                }
				$modalInstance.close({motive: motiveText,idMotive:selectedMotive});
			}
		};
		
		$scope.importSelected = function(batch){
			var promises = [];
            $scope.protoProfiles[batch.id].forEach(function(sample) {
				if (sample.selected) {
					promises.push(updateStatus(sample, 'Imported', batch).then(function() {
                        sample.selected = false;
                    }));
				}
			});
            $q.all(promises).then(function() {
                alertService.success({message: 'Se ha realizado el cambio de estado satisfactoriamente'});
            }, function(response) {
                alertService.error({message: ' Hubo un error: ' + response.toString()});
            });
		};

        $scope.desktopSearchResults = function (batch) {
            var profileIdToMatch = $scope.protoProfiles[batch.id][0].id;
            if (batch.desktopSearch) {
                $modal.open({
                    templateUrl: '/assets/javascripts/bulkupload/desktopSearchResults.html',
                    controller: 'desktopSearchController',
                    resolve: {
                        profileId: function () {
                            return profileIdToMatch;
                        }
                    }
                }).result.then(function () {
                    closeDesktopResults();
                });
            }
        };


        function closeDesktopResults(){
            //remove profile and matches
        }

        $scope.importBatch = function(batch){
            batch.isProcessing = true;
            var protoprofilesFromBatch= $scope.protoProfiles[batch.id];
            var replicateAll = false;
            var idsToReplicate = [];
            if(!_.isUndefined(protoprofilesFromBatch)){
                protoprofilesFromBatch.forEach(function(sample) {
                    if (sample.replicate) {
                        idsToReplicate.push(sample.id);
                    }
                });
			}
            if (batch.desktopSearch) {idsToReplicate = [];} // If the batch is for desktop search, we don't replicate to superior instance
            bulkuploadService.changeBatchStatus(batch.id, 'Imported',idsToReplicate,replicateAll).then(function() {
                batch.isProcessing = false;
                getBatchProtoProfiles(batch);
                alertService.success({message: 'Se ha importado el lote exitosamente.'});
            }, function(response) {
                batch.isProcessing = false;
                getBatchProtoProfiles(batch);
                alertService.error({message: ' Hubo un error: ' + response.data});
            });
        };
        $scope.deleteBatch = function(batch){
            $scope.isProcessing = true;
            bulkuploadService.deleteBatch(batch.id).then(function() {
                $scope.isProcessing = false;
                getAllBatches();
                alertService.success({message: 'Se ha eliminado el lote.'});
            }, function(response) {
                $scope.isProcessing = false;
                alertService.error({message: ' Hubo un error: ' + response.data.message});
            });
        };
        $scope.doDeleteBatch = function (confirmRes,batch) {
            if (!confirmRes) {
                return;
            }
            $scope.deleteBatch(JSON.parse(batch));
        };
		$scope.selectAll = function(batchId){
			bulkuploadService.selectAll(batchId, $scope.protoProfiles[batchId], toogle, 'Approved');
			toogle = !toogle;
		};
		
		$scope.setMismatch = function(id, batchId) {
			var pp = $scope.protoProfiles[batchId].filter(function(x){return x.id === id;})[0];
			var strigencyModalInstance = $modal.open({
				templateUrl:'/assets/javascripts/profiles/views/stringency-modal.html',
				controller: 'stringencyModalController',
				resolve: {
					data: function () {
						return {
							subcategory: {id: pp.category},
							profile: pp.sampleName,
							subcatsRel: pp.matchingRules,
							mismatches: pp.mismatches
						};
					}
				}
			}); 
			
			strigencyModalInstance.result.then(function (ret) {
				console.log(ret);
				$scope.subcatsRel = ret.subcatsRel;
				$scope.mismatches = ret.mismatches;
				
				bulkuploadService.updateProtoProfileRulesMismatch(id, ret.subcatsRel, ret.mismatches).then(function(response) {	
					if(response.data === id) {
						getBatchItem(id, batchId);
					}
					else {
//						alert('Hubo un error: ' + response.data);
						alertService.error({message: ' Hubo un error: ' + response.data});
					}
				});
				
			}, function(){});
		};
        $scope.editData = function(sample, action, batchId) {
            if (action === 'edit') { $scope.edit = sample.id; }
            if (action === 'cancel') {$scope.edit = ''; }

            if (action === 'save') {

                bulkuploadService.updateProtoProfileData(sample.id, $scope.editedSubcats[sample.id]).then(function(response) {
                    if (response.data.error) { alertService.error({message: response.data.error}); }
                    else {
                        response.data.replicate =  $scope.subcategory[response.data.category].replicate;
                        response.data.replicateDisabled =  !$scope.subcategory[response.data.category].replicate;
                        if($scope.subcategory[response.data.category].replicate){
                            response.data.replicate  = false;
                            response.data.replicateDisabled =  false;
                        }else{
                            response.data.replicate = false;
                            response.data.replicateDisabled =  true;
                        }
                        $scope.protoProfiles[batchId] = $scope.protoProfiles[batchId].map(function(x){
                            return (x.id === sample.id)? response.data: x;
                        });
                    }

                    $scope.edit = '';
                }, function(response) {
                    alertService.error({message: 'Hubo un error: ' + response.data.message});
                });


            }

        };

        var aprobados = function (batchId) {
                $scope.protoProfiles[batchId].todosAprobados = true;

                $scope.protoProfiles[batchId].forEach(function (n) {
                    if (n.status === 'Approved') {
                        $scope.protoProfiles[batchId].todosAprobados = false;
                    }
                });
        };

        $scope.show = function (index) {
            $scope.activeOption = index;
            console.log(index);
                getAllBatches();
        };

	}
	
	return Step2Controller;
});

