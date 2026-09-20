import type { AuthenticatedUser } from '../auth/domain/authentication'
import { copy } from '../product-copy'

interface ProfileProps {
  user: AuthenticatedUser
}

export function Profile({ user }: ProfileProps) {
  return (
    <section className="page-block" aria-labelledby="profile-title">
      <h1 id="profile-title">{copy.profile.title}</h1>
      <p className="page-lead">{copy.profile.lead}</p>
      <dl className="profile-data">
        <div>
          <dt>{copy.profile.email}</dt>
          <dd>{user.email}</dd>
        </div>
      </dl>
      <p className="page-note">{copy.profile.note}</p>
    </section>
  )
}
