define([], function() {
'use strict';

function OperationLogLotService(playRoutes, $http) {
	
	var remote = playRoutes.controllers.OperationLogs;
	
	this.getTotalLots = function() {
		console.log('GET TOTAL LOTS');
		return $http.head('/operationLog');
		//return remote.getTotalLots().head();
	};

	this.getTotalLogs = function(search) {
		console.log('GET TOTAL LOGS');
		return $http.post('/operationLogLot', search);
		//return remote.getTotalLogs().post(search);
	};

	this.getLotsNames = function(page, pageSize) {
		console.log('GET LOTS NAMES');
		return $http.get('/operationLog', {params: {page: page, pageSize: pageSize}});
		//return remote.getLogLots(page, pageSize).get();
	};

	this.listLotEntries = function(id, page, pageSize) {
		console.log('LIST LOT ENTRIES');
		return remote.listLotEntries(id, page, pageSize).get();
	};

	this.checkLot= function(lotId) {
		console.log('CHECK LOT');
		return $http.get('/operationLog/' + lotId + '/verification');
		//return remote.checkLogLot(lotId).get();
	};
	
	this.searchLogs = function(search) {
		console.log('SEARCH LOGS');
		return $http.post('/operationLogSearch', search);		
		//return remote.searchLogs().post(search);
	};
}

return OperationLogLotService;

});