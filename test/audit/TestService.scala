package audit

import javax.inject.{Inject, Named, Singleton}

@Singleton
class TestService @Inject() (
                              @Named("logEntrySigner") val logSignerService: PEOSignerService,
                              @Named("profileSigner") val profileSignerService: PEOSignerService) {
}
