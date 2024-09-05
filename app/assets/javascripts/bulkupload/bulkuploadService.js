define(['lodash'], function(_) {
	'use strict';

	function BulkuploadService(playRoutes, $http) {

		this.getStatusMap = function() {
			return {'Invalid': 'Inválido', 'Incomplete': 'Incompleto', 
					'ReadyForApproval': 'Listo para aprobación', 'Approved': 'Aprobado', 
					'Disapproved': 'Desaprobado', 'Imported': 'Aceptado', 'Rejected': 'Rechazado'};
		};
        this.getSubcategories = function() {
			console.log('GET SUBCATEGORIES');
			return $http.get('/categories');
            //return playRoutes.controllers.Categories.list().get();
        };
        this.getBatchesStep1 = function() {
			console.log('GET BATCHES STEP 1');
			return $http.get('/bulkupload/step1/batches');
			//return playRoutes.controllers.BulkUpload.getBatchesStep1().get();
		};
        
        this.getBatchesStep2 = function(geneMapperId) {
			console.log('GET BATCHES STEP 2');
			return $http.get('/bulkupload/step2/batches', {
				params: { geneMapperId: geneMapperId }
			});
            //return playRoutes.controllers.BulkUpload.getBatchesStep2(geneMapperId).get();
        };
		
		this.getProtoProfilesStep1 = function(batchId, page, pageSize) {
			console.log('GET PROTO PROFILES STEP 1');
			return $http.get('/bulkupload/step1/protoprofiles', {
				params: {
					batchId: batchId,
					page: page,
					pageSize: pageSize
				}
			});
			//return playRoutes.controllers.BulkUpload.getProtoProfilesStep1(batchId, page, pageSize).get();
		};

        this.getProtoProfilesStep2 = function(geneMapperId, batchId, page, pageSize) {
			console.log('GET PROTO PROFILES STEP 2');
			return $http.get('/bulkupload/step2/protoprofiles', {
				params: {
					geneMapperId: geneMapperId,
					batchId: batchId,
					page: page,
					pageSize: pageSize
				}
			});
            //return playRoutes.controllers.BulkUpload.getProtoProfilesStep2(geneMapperId, batchId, page, pageSize).get();
        };
		
		this.changeStatus = function(sampleName, newStatus,replicate) {
			console.log('UPDATE PROTO PROFILE STATUS');
			return $http.post('/protoprofiles/' + sampleName + '/status', {
				status: newStatus,
				replicate: replicate
			});
			//return playRoutes.controllers.BulkUpload.updateProtoProfileStatus(sampleName, newStatus,replicate).post();
		};

		this.changeBatchStatus = function(idBatch, newStatus,idsNotToReplicate,replicateAll) {
			if(idsNotToReplicate===undefined) {
                idsNotToReplicate = [];
            }
            if(replicateAll===undefined){
                replicateAll = false;
			}
			console.log('UPDATE BATCH STATUS');
			return $http.post('/protoprofiles/multiple-status', {
				idBatch: idBatch,
				status: newStatus,
				replicateAll: replicateAll,
				idsNotToReplicate: idsNotToReplicate
			});
			//return playRoutes.controllers.BulkUpload.updateBatchStatus(idBatch, newStatus, replicateAll).post(idsNotToReplicate);
		};
		
		this.getProtoProfileBySampleId = function(sampleId) {
			console.log('GET PROTO PROFILE BY SAMPLE ID');
			return $http.get('/bulkupload/protoprofiles/' + sampleId);
			//return playRoutes.controllers.BulkUpload.getProtoProfileById(sampleId).get();
		};
		
		this.updateProtoProfileData = function(sampleName, newCat) {
			console.log('UPDATE PROTO PRO FILE DATA');
			return $http.post('/protoprofiles/' + sampleName + '/subcategory', {
				category: newCat
			});			
			//return playRoutes.controllers.BulkUpload.updateProtoProfileData(sampleName, newCat).post();
		};
		
		this.selectAll = function(batchId, protoProfiles, toogle, status) {
			protoProfiles.forEach(function(x) { 
				if (x.status === status) { x.selected = toogle; }
			});
		};
		
		this.rejectProtoProfile = function(id, motive,idMotive) {
			console.log('REJECT PROTO PRO FILE');
			return $http.post('/protoprofiles-reject/' + id, {
				motive: motive,
				idMotive: idMotive
			});			
			//return playRoutes.controllers.BulkUpload.rejectProtoProfile(id, motive,idMotive).post();
		};
		
		this.updateProtoProfileRulesMismatch = function(id, subcatsRel, mismatches) {
			console.log('UPDATE PROTO PRO FILE RULES MISMATCH');			
			return $http.post('/protoprofiles/matchingrules', {
				id: id,
				matchingRules: subcatsRel,
				mismatches: mismatches
			});
			//return playRoutes.controllers.BulkUpload.updateProtoProfileRulesMismatch().post({id: id, matchingRules: subcatsRel, mismatches: mismatches});
		};

        this.deleteBatch = function(idBatch) {
			console.log('DELETE BATCH');
			return $http.delete('/bulkupload/' + idBatch);
            //return playRoutes.controllers.BulkUpload.deleteBatch(idBatch).delete();
        };

        this.getMotives = function()  {
			var motiveTypeReject =  1;
            return playRoutes.controllers.MotiveController.getMotives(motiveTypeReject,false).get();
        };

        this.fillRange = function(locusById,isOutOfLadder,l) {
			l.minAlleleValue = locusById[l.locus].minAlleleValue;
			l.maxAlleleValue = locusById[l.locus].maxAlleleValue;
            l.chromosome = locusById[l.locus].chromosome;

            l.isOutOfLadder = isOutOfLadder;

			if(!_.isUndefined(l.chromosome) && l.chromosome!=='MT' && l.chromosome!=='XY' && !_.isUndefined(l.maxAlleleValue)){
				l.range = "<"+l.minAlleleValue+", "+l.minAlleleValue+"-"+l.maxAlleleValue+", >"+l.maxAlleleValue;
			}
        };

		this.searchBatch = function (filter){
			return $http.get('/bulkupload/batches', { params: { filter: filter } });
			//return playRoutes.controllers.BulkUpload.searchBatch(filter).get();
		};
	}

	return BulkuploadService;
});