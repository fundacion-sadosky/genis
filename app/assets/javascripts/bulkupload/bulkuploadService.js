define(['lodash'], function(_) {
	'use strict';

	function BulkuploadService(playRoutes) {

		this.getStatusMap = function() {
			return {'Invalid': 'Inválido', 'Incomplete': 'Incompleto', 
					'ReadyForApproval': 'Listo para aprobación', 'Approved': 'Aprobado', 
					'Disapproved': 'Desaprobado', 'Imported': 'Aceptado', 'Rejected': 'Rechazado'};
		};
        this.getSubcategories = function() {
            return playRoutes.controllers.Categories.list().get();
        };
        this.getBatchesStep1 = function() {
			return playRoutes.controllers.BulkUpload.getBatchesStep1().get();
		};
        
        this.getBatchesStep2 = function(geneMapperId) {
            return playRoutes.controllers.BulkUpload.getBatchesStep2(geneMapperId).get();
        };
		
		this.getProtoProfilesStep1 = function(batchId, page, pageSize) {
			return playRoutes.controllers.BulkUpload.getProtoProfilesStep1(batchId, page, pageSize).get();
		};

        this.getProtoProfilesStep2 = function(geneMapperId, batchId, page, pageSize) {
            return playRoutes.controllers.BulkUpload.getProtoProfilesStep2(geneMapperId, batchId, page, pageSize).get();
        };
		
		this.changeStatus = function(sampleName, newStatus,replicate, desktopSearch) {
			if (typeof desktopSearch === 'undefined') {
				desktopSearch = false;
			}
			return playRoutes.controllers.BulkUpload.updateProtoProfileStatus(sampleName, newStatus, replicate, desktopSearch).post();
		};

		this.changeBatchStatus = function(idBatch, newStatus,idsNotToReplicate,replicateAll) {
			if(idsNotToReplicate===undefined) {
                idsNotToReplicate = [];
            }
            if(replicateAll===undefined){
                replicateAll = false;
			}
			return playRoutes.controllers.BulkUpload.updateBatchStatus(idBatch, newStatus,replicateAll).post(idsNotToReplicate);
		};
		
		this.getProtoProfileBySampleId = function(sampleId) {
			return playRoutes.controllers.BulkUpload.getProtoProfileById(sampleId).get();
		};
		
		this.updateProtoProfileData = function(sampleName, newCat) {
			return playRoutes.controllers.BulkUpload.updateProtoProfileData(sampleName, newCat).post();
		};
		
		this.selectAll = function(batchId, protoProfiles, toogle, status) {
			protoProfiles.forEach(function(x) { 
				if (x.status === status) { x.selected = toogle; }
			});
		};
		
		this.rejectProtoProfile = function(id, motive,idMotive) {
			return playRoutes.controllers.BulkUpload.rejectProtoProfile(id, motive,idMotive).post();
		};
		
		this.updateProtoProfileRulesMismatch = function(id, subcatsRel, mismatches) {
			return playRoutes.controllers.BulkUpload.updateProtoProfileRulesMismatch().post({id: id, matchingRules: subcatsRel, mismatches: mismatches});
		};

        this.deleteBatch = function(idBatch) {
            return playRoutes.controllers.BulkUpload.deleteBatch(idBatch).delete();
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
			return playRoutes.controllers.BulkUpload.searchBatch(filter).get();
		};
	}

	return BulkuploadService;
});