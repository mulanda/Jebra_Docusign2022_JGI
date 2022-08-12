package providers.docusign

import user.User

/**
 * Created by Daudi Chilongo on 08/12/2022.
 * Copyright (c) 2022 Jebra LTD. All rights reserved.
 */
trait DocusignUser {

  implicit class DocusignSignWith(user: User) {

    def docusignSigner(recipientId: Long): DocusignSigner = {
      DocusignSigner(
        recipientId = recipientId,
        name = user.name.get,
        email = user.email)
    }

  }


}