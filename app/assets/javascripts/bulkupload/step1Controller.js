define(['lodash'], function(_) {
	'use strict';

	function Step1Controller($scope, $location, bulkuploadService, $upload, profileDataService, cryptoService, $log, $modal, appConf, alertService, $q,locusService) {
		$scope.statusMap = bulkuploadService.getStatusMap();
		$scope.protoProfiles = {};
		$scope.batches = [];
		$scope.editedSubcats = {};
    $scope.pageSize = 50;
    $scope.editedIds = [];
		var toogle = true;
		$scope.pendientes = false;

    localStorage.removeItem("searchPedigree");
    localStorage.removeItem("searchMatches");
    localStorage.removeItem("searchPedigreeMatches");

    var getAllBatches = function() {
      $scope.isProcessing = true;
      return bulkuploadService
        .getBatchesStep1()
        .then(
          function(response) {
            $scope.batches = response.data;
            $scope.batchesById = _.keyBy($scope.batches, 'id');
            $scope.batches.forEach(
              function(batch){
                batch.pageSize = $scope.pageSize;
                batch.page = 1;
              }
            );
            $scope.isProcessing = false;
          }
        );
    };
    var loadLocus = function() {
        return locusService
          .listFull()
          .then(
            function(response) {
              $scope.locus = response.data;
              $scope.locusById = _.keyBy(
                _.map($scope.locus, function (o) {  return o.locus;}),
                'id'
              );
        });
    };
    getAllBatches();
    loadLocus();
		profileDataService.getCategories().then(
      function(response) {
        $scope.categories = response.data;
      }
    );

		var upload = function(files, label, analysis) {
      var file = files;
			$log.info('upload a file');
			$scope.isProcessing = true;
      var url;
			if(_.isUndefined(label)){
        url = cryptoService.encryptBase64(
          '/bulkuploader?label= '+'&analysisType=' +analysis
        );
      } else {
        url = cryptoService.encryptBase64(
          '/bulkuploader?label='+ label +'&analysisType=' +analysis
        );
      }
			file.upload = $upload.upload({
				url: url, 
				method: 'POST',
				file: file
			});
			file.upload.then(function(response) {
					file.result = response.data;
					if (file.result.message) { alertService.error({message: file.result.message}); }
					else {
						var batchId = file.result;
                        getAllBatches().then(function() {
                            getBatchProtoProfiles(batchId,1,$scope.pageSize).then(function() {
                                $scope.openAccordian = batchId;
                                $scope.addedBatchId = batchId;
                            });
                        });
					}
                    $scope.isProcessing = false;
			}, function(response) {
				if (response.status > 0){
					alertService.error({message: ((response.data && response.data.message)? response.data.message: 'Hubo un error, por favor verifique el log')});
//					alert(response.status + ': ' + response.data);
				}
				$scope.isProcessing = false;
			});
		};
		
		var getBatchProtoProfiles = function(batchId, page, pageSize) {
            $scope.isProcessing = true;
            return bulkuploadService.getProtoProfilesStep1(batchId, page - 1, pageSize).then(function(response) {
                for(var i = 0; i < response.data.length; i++){
                    _.forEach(response.data[i].genotypification,_.partial(bulkuploadService.fillRange,$scope.locusById,locusService.isOutOfLadder));
                }

				$scope.protoProfiles[batchId] = response.data;
                aprobados(batchId);
                $scope.isProcessing = false;
			}, function() { $scope.isProcessing = false; });
		};

		var updateStatus = function(sample, status, batchId) {
            $scope.isProcessing = true;
            var deferred = $q.defer();
            bulkuploadService.changeStatus(sample.id, status).then(function(response) {
                if(response.data.length > 0) {
                    deferred.reject(response.data);
                } else {
                    if (status === 'Approved') {
                        for (var i in $scope.batches) {
                            var batch = $scope.batches[i];
                            if (batch.id === batchId) {
                                batch.approvedAnalysis++;
                                sample.status = 'Approved';

                            }
                        }
                    }
                    if (status === 'Disapproved') {
                        for (var j in $scope.batches) {
                            var batchj = $scope.batches[j];
                            if (batchj.id === batchId) {
                                batchj.rejectedAnalysis++;
                                sample.status = 'Disapproved';

                            }
                        }
                    }
                    deferred.resolve();
                }
                aprobados(batchId);
                $scope.isProcessing = false;
			}, function(response) {
                deferred.reject(response.data);
                $scope.isProcessing = false;
            });
            return deferred.promise;
        };

		$scope.changePage = function(batch) {
            getBatchProtoProfiles(batch.id, batch.page, batch.pageSize);
		};
        
		$scope.showProtoProfiles = function(batch, ev) {
			if ($scope.protoProfiles[batch.id] && ev.currentTarget &&
					ev.currentTarget.attributes['class'].value.indexOf('collapsed') === -1 ) {return;}
			getBatchProtoProfiles(batch.id, batch.page, batch.pageSize);
		};
		
		$scope.approve = function(sample, batchId) {
			updateStatus(sample, 'Approved', batchId).then(function() {
                alertService.success({message: 'Se ha realizado el cambio de estado satisfactoriamente'});
                sample.selected = false;
            }, function(response) {
                alertService.error({message: 'Hubo un error: ' + response.toString()});
            });
		};
		
		$scope.disapprove = function(sample, batchId) {
			updateStatus(sample, 'Disapproved', batchId).then(function() {
                alertService.success({message: 'Se ha realizado el cambio de estado satisfactoriamente'});
                sample.selected = false;
            }, function(response) {
                alertService.error({message: 'Hubo un error: ' + response.toString()});
            });
		};
		
		function editMetaData(idSample){
			$modal.open({
				templateUrl:'/assets/javascripts/profiledata/profile-data.html',
				controller: 'protoProfileDataController',
				resolve: {
					rp: function () {
						return {
							samplecode: appConf.protoProfileGlobalCodeDummy + idSample
						};
					}
				}
			}); 
		}
		
		$scope.editData = function(sample, action, batchId) {
			if (action === 'edit') { $scope.edit = sample.id; $scope.editedIds.push(sample.id);}
			if (action === 'cancel') {$scope.edit = ''; }
			if (action === 'metadata') {editMetaData(sample.id, batchId);}
			
			if (action === 'save') {
				bulkuploadService.updateProtoProfileData(sample.id, $scope.editedSubcats[sample.id]).then(function(response) {
					if (response.data.error) { alertService.error({message: response.data.error}); }
					else {
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
		
		$scope.approveBatch = function(batch){
            $scope.isProcessing = true;
            bulkuploadService.changeBatchStatus(batch.id, 'Approved').then(function() {
                $scope.isProcessing = false;
                getBatchProtoProfiles(batch.id, batch.page, batch.pageSize);
                alertService.success({message: 'Se ha aprobado el lote exitosamente.'});
            }, function(response) {
                $scope.isProcessing = false;
                getBatchProtoProfiles(batch.id, batch.page, batch.pageSize);
                alertService.error({message: 'Hubo un error: ' + response.data});
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
                alertService.error({message: 'Hubo un error: ' + response.data.message});
            });
        };
        $scope.doDeleteBatch = function (confirmRes,batch) {
            if (!confirmRes) {
                return;
            }
            $scope.deleteBatch(JSON.parse(batch));
        };
        $scope.approveSelected = function(batchId){
            var promises = [];
            $scope.protoProfiles[batchId].forEach(function(sample) {
                if (sample.selected) {
                    promises.push(updateStatus(sample, 'Approved', batchId).then(function() {
                        sample.selected = false;
                    }));
                }
            });
            $q.all(promises).then(function() {
                alertService.success({message: 'Se ha realizado el cambio de estado satisfactoriamente'});
            }, function(response) {
                alertService.error({message: 'Hubo un error: ' + response.toString()});
            });
        };
		
		$scope.selectAll = function(batchId){
			bulkuploadService.selectAll(batchId, $scope.protoProfiles[batchId], toogle, 'ReadyForApproval');
			toogle = !toogle;
		};

        $scope.addLote = function () {
            $modal.open({
                templateUrl:'/assets/javascripts/bulkupload/addLote.html',
                controller : 'addLoteController'
              }).result.then(function(lote ){
                closeModalLote(lote);
            });
        };

        function closeModalLote(lote){
            upload(lote.files , lote.label, lote.analisis);
        }



        $scope.searchLote = function(filter) {
            $scope.isProcessing = true;
            console.error(filter);
            if(!filter){
                filter="";
            }
            bulkuploadService.searchBatch(filter).then(function(response) {
                if($scope.pendientes){
                    $scope.batches = response.data.filter(function (x) {
                        return x.totalForApprovalOrImport !== 0 || x.totalForIncomplete !== 0;
                    } );
                }else {
                        $scope.batches = response.data;
                }
                $scope.batchesById = _.keyBy($scope.batches, 'id');
                $scope.batches.forEach(function(batch){
                    batch.pageSize = $scope.pageSize;
                    batch.page = 1;
                });
                $scope.isProcessing = false;
            });
        };


        $scope.clean = function() {
            $scope.search = {input: ''};
            $scope.pendientes = false;
            getAllBatches();
        };

        var aprobados = function (batchId) {
            $scope.protoProfiles[batchId].todosAprobados = true;

                $scope.protoProfiles[batchId].forEach(function (n) {
                    if (n.status === 'ReadyForApproval') {
                        $scope.protoProfiles[batchId].todosAprobados = false;
                    }
                });
        };
	}


	return Step1Controller;
});

