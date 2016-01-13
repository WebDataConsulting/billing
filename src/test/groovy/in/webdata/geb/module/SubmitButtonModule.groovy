package in.webdata.geb.module

import geb.Module

class SubmitButtonModule extends Module {
	static content = {
		println " submit button ********"
		buttons { $('div.row > a.submit.add > span') }
	}
}

/*
class AddButtonModule extends Module {
	static content = {
		add { module ButtonModule }
		clickAdd { add.buttons.each {
				it.equals("ADD NEW") ? it.click() : ""
			}
		}
	}
}

class AddSubAccountButtonModule extends Module {
	static content = {
		println "calling button module 1****************"
		add { module ButtonModule }
		clickAddSubAcct { add.buttons.each {
			println "calling button module ****************"
				it.equals("ADD SUB-ACCOUNT") ? it.click() : ""
			}
		}
	}
}
*/
