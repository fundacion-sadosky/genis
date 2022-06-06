define([], function() {
'use strict';

function OperationLogLotService(playRoutes) {
	
	var remote = playRoutes.controllers.OperationLogs;
	
	this.getTotalLots = function() {
		return remote.getTotalLots().head();
	};

	this.getTotalLogs = function(search) {
		return remote.getTotalLogs().post(search);
	};

	this.getLotsNames = function(page, pageSize) {
		return remote.getLogLots(page, pageSize).get();
	};

	this.listLotEntries = function(id, page, pageSize) {
		return remote.listLotEntries(id, page, pageSize).get();
	};

	this.checkLot= function(lotId) {
		return remote.checkLogLot(lotId).get();
	};
	
	this.searchLogs = function(search) {
		return remote.searchLogs().post(search);
	};
}

return OperationLogLotService;

});