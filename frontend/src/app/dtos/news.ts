export interface News {
  id?: number;
  title: string;
  summary: string;
  text?: string;
  publishedAt?: string;
  image?: string;
}

export interface NewsPage {
  content: News[];
  totalPages: number;
  totalElements: number;
}
