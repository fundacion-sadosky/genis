define([], function() {
'use strict';

function OperationLogLotService(playRoutes, $http) {
	
	var remote = playRoutes.controllers.OperationLogs;
	
	this.getTotalLots = function() {
		return remote.getTotalLots().head();
	};

	this.getTotalLogs = function(search) {
		console.log('GET TOTAL LOGS');
		return $http.post('/operationLogLot', search);
		//return remote.getTotalLogs().post(search);
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
		console.log('SEARCH LOGS');
		return $http.post('/operationLogSearch', search);		
		//return remote.searchLogs().post(search);
	};
}

return OperationLogLotService;

});