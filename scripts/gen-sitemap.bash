# This script is for the one-off generation of a sitemap
# that will include all published articles.
aws s3api list-objects-v2 --profile membership --bucket manage-help-content --prefix PROD/articles/ --page-size 99999 \
| jq '.Contents | .[].Key' \
| tail -n +2 \
| sed "s/\"PROD\/articles/https:\/\/manage.theguardian.com\/help-centre\/article/g" \
| sed "s/.json\"//g" \
> sitemap.txt
