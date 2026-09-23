export type ProductView =
  | 'videos'
  | 'upload'
  | 'profile'
  | { kind: 'video-detail'; videoRef: string }

export function isDetailView(view: ProductView): view is { kind: 'video-detail'; videoRef: string } {
  return typeof view === 'object' && view.kind === 'video-detail'
}

export function sectionOf(view: ProductView): 'videos' | 'upload' | 'profile' {
  if (view === 'upload' || view === 'profile') {
    return view
  }
  return 'videos'
}
