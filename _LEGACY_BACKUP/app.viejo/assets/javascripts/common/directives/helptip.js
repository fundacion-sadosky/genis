define(['angular'], function(angular) {
  'use strict';

  var mod = angular.module('common.directives.helptip', ['jm.i18next']);
  mod.controller('HelpTipCtrl', ['$rootScope', '$scope', '$log','hotkeys', function($rootScope, $scope, $log,hotkeys) {

	this.toggle = function() {
		$rootScope.helpTipsDisplay = $rootScope.helpTipsDisplay === 'show' ? 'hide' : 'show';
        $scope.helpActive = $rootScope.helpTipsDisplay === 'show';
		$log.debug('helptips ' + $rootScope.helpTipsDisplay);
	};

	var myToggle = this.toggle;
	
	hotkeys.add({              
		combo: 'ctrl+alt+h',   
		description: 'tooltips',
		callback: function(){myToggle();}
		});
	
  }]);
  mod.directive('helpTip', ['$compile', '$rootScope', '$log', function($compile, $rootScope, $log) {
    return {
		restrict: 'A',
		link: function(scope, el, attrs) {

			el.on('$destroy', function(/*event*/) {
				el.popover('destroy');
			});

			$rootScope.$watch('helpTipsDisplay', function(value) {
				if(el.is(':visible')) {
					el.popover(value);	
				}
			});
            var helpTipScope = scope.$new(true);
            helpTipScope.key = attrs.helpTip;
            helpTipScope.mtImg = '<img src="/assets/images/mt.png"  />';
            helpTipScope.img = '';
            if(helpTipScope.key.includes("mt.mutation")){
                helpTipScope.img = helpTipScope.mtImg;
            }
			var helpTipContent = $compile('<div style="width: 230px;white-space: pre-wrap;" font-weight: normal" class="text-center">'+helpTipScope.img+'{{key | i18next}}</div>')(helpTipScope);
			
			$log.debug('crating help tip for key: ' + attrs.helpTip);
			el.popover({
				content : helpTipContent, 
				trigger: 'manual',
				html : true,
				placement : attrs.helpTipPlacement || 'right'});
      
		}
    };
  }]);

  return mod;
});
